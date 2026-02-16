"""GUI-конвертер .pt -> .sbmmodel для Android-приложения SBM AOI.

Запуск (Windows / PyCharm):
1) Установите зависимости из requirements.txt
2) Запустите этот файл
"""

from __future__ import annotations

import json
import shutil
import tempfile
from pathlib import Path
import tkinter as tk
from tkinter import filedialog, messagebox, ttk
import zipfile


class ConversionError(Exception):
    pass


def load_labels(labels_path: Path) -> list[str]:
    if not labels_path.exists():
        raise ConversionError(f"Файл классов не найден: {labels_path}")

    suffix = labels_path.suffix.lower()
    if suffix == ".txt":
        labels = [line.strip() for line in labels_path.read_text(encoding="utf-8").splitlines() if line.strip()]
        if not labels:
            raise ConversionError("labels.txt пустой")
        return labels

    if suffix in {".yaml", ".yml"}:
        try:
            import yaml
        except ImportError as exc:
            raise ConversionError("Для YAML нужен пакет pyyaml: pip install pyyaml") from exc

        data = yaml.safe_load(labels_path.read_text(encoding="utf-8"))
        names = data.get("names") if isinstance(data, dict) else None

        if isinstance(names, dict):
            labels = [str(name) for _, name in sorted(names.items(), key=lambda item: int(item[0]))]
        elif isinstance(names, list):
            labels = [str(name) for name in names]
        else:
            raise ConversionError("В YAML нет поля names (list или dict)")

        labels = [label.strip() for label in labels if label and str(label).strip()]
        if not labels:
            raise ConversionError("Список классов в YAML пуст")
        return labels

    raise ConversionError("Поддерживаются только labels .txt/.yaml/.yml")


def export_to_onnx(pt_path: Path, input_size: int) -> Path:
    try:
        from ultralytics import YOLO
    except ImportError as exc:
        raise ConversionError(
            "Не найден ultralytics. Установите зависимости: pip install -r requirements.txt"
        ) from exc

    model = YOLO(str(pt_path))
    exported = model.export(format="onnx", imgsz=input_size, simplify=True)
    onnx_path = Path(exported)
    if not onnx_path.exists():
        raise ConversionError(f"Экспорт ONNX не удался: {onnx_path}")
    return onnx_path


def build_sbmmodel(
    onnx_path: Path,
    labels: list[str],
    output_file: Path,
    model_name: str,
    input_size: int,
    conf: float,
    iou: float,
) -> None:
    output_file.parent.mkdir(parents=True, exist_ok=True)

    config = {
        "name": model_name or onnx_path.stem,
        "input_size": input_size,
        "type": "yolo_detect",
        "confidence_default": conf,
        "iou_default": iou,
        "version": 1,
    }

    with tempfile.TemporaryDirectory() as tmp:
        tmp_dir = Path(tmp)
        model_file = tmp_dir / "model.onnx"
        labels_file = tmp_dir / "labels.txt"
        config_file = tmp_dir / "config.json"

        shutil.copy2(onnx_path, model_file)
        labels_file.write_text("\n".join(labels) + "\n", encoding="utf-8")
        config_file.write_text(json.dumps(config, ensure_ascii=False, indent=2), encoding="utf-8")

        with zipfile.ZipFile(output_file, mode="w", compression=zipfile.ZIP_DEFLATED) as zipf:
            zipf.write(model_file, arcname="model.onnx")
            zipf.write(labels_file, arcname="labels.txt")
            zipf.write(config_file, arcname="config.json")


class ConverterApp:
    def __init__(self, root: tk.Tk) -> None:
        self.root = root
        self.root.title("PT -> SBMModel Converter")
        self.root.geometry("760x420")

        self.pt_path = tk.StringVar()
        self.labels_path = tk.StringVar()
        self.output_path = tk.StringVar()
        self.model_name = tk.StringVar(value="MyModel")
        self.input_size = tk.StringVar(value="640")
        self.conf = tk.StringVar(value="0.35")
        self.iou = tk.StringVar(value="0.50")

        self._build_ui()

    def _build_ui(self) -> None:
        main = ttk.Frame(self.root, padding=16)
        main.pack(fill=tk.BOTH, expand=True)

        def row(label: str, var: tk.StringVar, browse_cmd, file_types):
            ttk.Label(main, text=label).pack(anchor="w")
            line = ttk.Frame(main)
            line.pack(fill=tk.X, pady=(0, 8))
            ttk.Entry(line, textvariable=var).pack(side=tk.LEFT, fill=tk.X, expand=True)
            ttk.Button(line, text="Выбрать", command=lambda: browse_cmd(var, file_types)).pack(side=tk.LEFT, padx=(8, 0))

        row("1) PT веса модели (*.pt)", self.pt_path, self._browse_open, [("PyTorch model", "*.pt")])
        row(
            "2) Файл классов датасета (labels.txt / data.yaml)",
            self.labels_path,
            self._browse_open,
            [("Labels/YAML", "*.txt *.yaml *.yml")],
        )

        ttk.Label(main, text="3) Выходной файл (*.sbmmodel)").pack(anchor="w")
        line_out = ttk.Frame(main)
        line_out.pack(fill=tk.X, pady=(0, 8))
        ttk.Entry(line_out, textvariable=self.output_path).pack(side=tk.LEFT, fill=tk.X, expand=True)
        ttk.Button(line_out, text="Сохранить как", command=self._browse_save).pack(side=tk.LEFT, padx=(8, 0))

        grid = ttk.Frame(main)
        grid.pack(fill=tk.X, pady=(8, 8))

        self._field(grid, "Имя модели", self.model_name, 0)
        self._field(grid, "Input size", self.input_size, 1)
        self._field(grid, "Confidence", self.conf, 2)
        self._field(grid, "IoU", self.iou, 3)

        ttk.Button(main, text="Конвертировать", command=self.convert).pack(anchor="e", pady=(8, 0))

        help_text = (
            "Формат .sbmmodel содержит model.onnx + labels.txt + config.json.\n"
            "Этот формат напрямую поддерживается загрузчиком модели в Android приложении."
        )
        ttk.Label(main, text=help_text, foreground="#666666").pack(anchor="w", pady=(16, 0))

    def _field(self, parent: ttk.Frame, label: str, var: tk.StringVar, col: int) -> None:
        cell = ttk.Frame(parent)
        cell.grid(row=0, column=col, padx=6, sticky="ew")
        parent.columnconfigure(col, weight=1)
        ttk.Label(cell, text=label).pack(anchor="w")
        ttk.Entry(cell, textvariable=var).pack(fill=tk.X)

    @staticmethod
    def _browse_open(var: tk.StringVar, file_types):
        selected = filedialog.askopenfilename(filetypes=file_types)
        if selected:
            var.set(selected)

    def _browse_save(self):
        selected = filedialog.asksaveasfilename(
            defaultextension=".sbmmodel",
            filetypes=[("SBM model", "*.sbmmodel")],
        )
        if selected:
            self.output_path.set(selected)

    def convert(self) -> None:
        try:
            pt_path = Path(self.pt_path.get().strip())
            labels_path = Path(self.labels_path.get().strip())
            output_path = Path(self.output_path.get().strip())
            model_name = self.model_name.get().strip() or pt_path.stem
            input_size = int(self.input_size.get().strip())
            conf = float(self.conf.get().strip())
            iou = float(self.iou.get().strip())

            if not pt_path.exists() or pt_path.suffix.lower() != ".pt":
                raise ConversionError("Выберите корректный .pt файл")
            if not output_path.name.lower().endswith(".sbmmodel"):
                raise ConversionError("Выходной файл должен иметь расширение .sbmmodel")

            labels = load_labels(labels_path)
            onnx_path = export_to_onnx(pt_path, input_size)
            build_sbmmodel(onnx_path, labels, output_path, model_name, input_size, conf, iou)

            messagebox.showinfo("Готово", f"Конвертация завершена:\n{output_path}")
        except Exception as exc:
            messagebox.showerror("Ошибка", str(exc))


def main() -> None:
    root = tk.Tk()
    style = ttk.Style(root)
    if "vista" in style.theme_names():
        style.theme_use("vista")
    ConverterApp(root)
    root.mainloop()


if __name__ == "__main__":
    main()
