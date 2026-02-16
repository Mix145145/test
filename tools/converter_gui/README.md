# PT -> SBMModel converter (Windows GUI)

## Что делает
Скрипт `pt_to_sbmmodel_gui.py` конвертирует веса `.pt` в формат `.sbmmodel`, который открывается в Android-приложении.

`.sbmmodel` — это ZIP-пакет с файлами:
- `model.onnx`
- `labels.txt`
- `config.json`

## Запуск в PyCharm (Windows)
1. Откройте папку `tools/converter_gui` как проект или как часть текущего проекта.
2. Создайте Python Interpreter (рекомендуется venv, Python 3.10+).
3. Установите зависимости:
   ```bash
   pip install -r requirements.txt
   ```
4. Запустите `pt_to_sbmmodel_gui.py`.

## Как пользоваться
1. Выберите `*.pt` файл модели.
2. Выберите файл классов датасета:
   - `labels.txt` (один класс на строку), или
   - `data.yaml` / `data.yml` (поле `names`).
3. Выберите, куда сохранить `*.sbmmodel`.
4. Нажмите **Конвертировать**.

## Совместимость с приложением
Формат соответствует загрузчику модели в проекте:
- приложение принимает `.sbmmodel` и извлекает из него `model.onnx`, `labels.txt`, `config.json`.
