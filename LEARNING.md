# 学習サイクルとPC側ワークフロー設計

## 学習サイクル概要
1. **データ収集**
   - Androidアプリの登録機能からロゴ画像とメタ情報（ブランド名・備考など）を蓄積。
   - 定期的に端末からPCへロゴ画像一式を吸い上げ、ブランド別にフォルダ分類 (`dataset/BRAND_X/*.jpg`)。
   - 公開データセット（OpenLogo, LogoDet-3K 等）や社内素材も統合。

2. **前処理・データ拡張**
   - 画像サイズ統一（例: 224x224）・色空間変換・正規化。
   - 角度回転・左右反転・明度補正などのデータ拡張をブランド毎に適用。
   - データ数の少ないブランドはAugmentationで補完。

3. **転移学習**
   - MobileNet/EfficientNetなどImageNetベースのモデルを利用。最終層をブランド数に合わせて置換。
   - まずベースモデルを固定（freeze）し、高速にヘッド部分を学習。
   - 精度が頭打ちになったら浅い層から徐々に解凍しfine-tuning。

4. **評価・微調整**
   - 検証用データセットで精度、混同行列、Top-K精度を評価。
   - 必要に応じてクラスウェイト、データ拡張や学習率を調整。

5. **TFLite化**
   - 学習済みKeras/SavedModelを `tf.lite.TFLiteConverter` で `.tflite` に変換。
   - 端末性能に応じて量子化（Float16/Int8）を検討。

6. **配布・検証**
   - `logo_classifier.tflite` を `app/src/main/assets/` に追加してアプリを再ビルド。
   - QA端末で動作確認（推論速度・メモリ・認識精度・履歴保存等）。
   - 問題なければバージョン管理＋配布。

## PC側ワークフロー設計

### ディレクトリ構成例
```
training/
  ├── data/
  │   ├── raw/                # 端末から吸い上げた生データ
  │   ├── processed/          # 前処理済みデータ
  │   └── splits/             # train/val/test の分割
  ├── notebooks/              # 探索的データ分析・実験ノート
  ├── scripts/
  │   ├── preprocess.py       # サイズ統一・データ拡張など
  │   ├── train.py            # 転移学習本体（CLI化）
  │   ├── evaluate.py         # 評価・指標算出
  │   └── export_tflite.py    # SavedModel→TFLite変換
  ├── models/                 # 学習済みモデル（チェックポイント/Export）
  ├── logs/                   # TensorBoardログ
  └── configs/
      ├── base.yaml           # 学習パラメータ設定（学習率/バッチサイズなど）
      └── brands.yaml         # ブランドID定義（アプリと共有）
```

### ワークフロー
1. **データ同期**
   - 端末からADBでロゴ画像ディレクトリを吸い上げる。例：
     ```bash
     # 端末側保存先（アプリ実装に合わせてパスを調整）
     adb shell ls /storage/emulated/0/Android/data/com.example.logoocr.debug/files/logos
     adb pull /storage/emulated/0/Android/data/com.example.logoocr.debug/files/logos ./training/data/raw
     ```
   - 端末のデータが内部ストレージ（`/data/data/...`）にある場合はRoot/デバッグビルドが必要になるため、アプリ側で外部ストレージ配下に保存する運用が扱いやすい。
   - 吸い上げたファイルをブランド名でフォルダ整理（`training/data/raw/BRAND_NAME/*.jpg`）。ラベル情報をCSVやJSONにまとめておくと学習スクリプトで扱いやすい。

2. **前処理スクリプト実行**
   ```bash
   python scripts/preprocess.py \
     --input-dir data/raw \
     --output-dir data/processed \
     --img-size 224 \
     --augment-config configs/augment.yaml
   ```
   - train/val/test 分割ファイルを出力し `config` で参照。

3. **学習実行**
   ```bash
   python scripts/train.py \
     --config configs/base.yaml \
     --data-dir data/processed \
     --output-dir models/exp_YYYYMMDD \
     --tensorboard-dir logs/exp_YYYYMMDD
   ```
   - Hydra や argparse で設定管理。  
   - 学習完了後 `models/exp_*/saved_model/` を生成。

4. **評価**
   ```bash
   python scripts/evaluate.py \
     --model-path models/exp_YYYYMMDD/saved_model \
     --data-dir data/processed \
     --split val
   ```
   - 指標を出力し、既存モデルとの差分を確認。

5. **TFLiteエクスポート**
   ```bash
   python scripts/export_tflite.py \
     --model-path models/exp_YYYYMMDD/saved_model \
     --output-path models/exp_YYYYMMDD/logo_classifier.tflite \
     --quantize float16
   ```
   - 必要なら代表的データセットを指定し、INT8量子化。

6. **アプリへ組み込み**
   - `models/exp_YYYYMMDD/logo_classifier.tflite` をリポジトリの `app/src/main/assets/` にコピー。
   - `app` の `build.gradle` バージョンを上げ、`./gradlew assembleDebug` などでビルド。

7. **バージョン管理**
   - モデルのメタ情報（学習データセット、ハイパーパラメータ、評価結果）を `models/exp_*/metadata.json` に保存し、Git LFS や社内モデル管理で管理。

### システム補足
- **自動化の候補**  
  - CI/CD（GitHub Actions, Jenkins 等）で、データ更新→学習→TFLite化→成果物アップロードまでをパイプライン化。
  - 代表的な性能確認を自動で実行し、指定の閾値を満たしたモデルだけを採用。

- **ブランドIDの同期**  
  - アプリのRoomテーブルとPC側学習環境でブランドIDがズレないように `configs/brands.yaml` 等で一元管理。

- **データの安全性**  
  - 端末から吸い上げる画像は社内管理ストレージに保管し、アクセス権限を制御。

この設計により、端末内で収集したロゴ画像を定期的に模型化し、新しい `.tflite` を生成・配布するサイクルを回せるようになります。
