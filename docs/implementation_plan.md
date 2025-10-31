# 開発進行プラン

## フェーズA: プロジェクト基盤整備
1. Gradle/ComposeベースのAndroidアプリ雛形を作成（`com.example.logoocr` などのパッケージ）。
2. マルチモジュールを見据え、`app` モジュール構成と共通依存管理（`libs.versions.toml`）を準備。
3. Jetpack Composeナビゲーション、Material3、ViewModel（Hilt導入下準備）を組み込む。
4. CI向けに `./gradlew lint ktlintCheck` など基本タスクが走る状態を確認。

## フェーズB: ドメイン / データレイヤー設計
1. Roomエンティティ（Brand / Logo / RecognitionResult）とDAO定義。
2. Repositoryインタフェースと実装（Roomベース）を準備。
3. サンプルデータ流し込み用のSeedスクリプト or 初期化ロジックを追加。

## フェーズC: UIフローの骨組み
1. メイン（Camera）、ロゴ登録、認識確認、履歴画面のComposeスクリーンを仮実装。
2. Navigation Graphで画面遷移を定義。
3. ViewModelにてRoomデータをFlowで監視し、UIと連携。

## フェーズD: コア機能統合
1. CameraXプレビューと画像キャプチャ、鏡像補正（OpenCV/Matrix操作）を組み込み。
2. ロゴ特徴抽出（TFLite + embedding）とオンデバイスkNNマッチングの暫定実装。
3. OCR（Tesseract/ML Kit TFLite）呼び出しのラッパーを作成。
4. 認識結果検証UIとDB保存パイプラインを接続。

## フェーズE: 仕上げ
1. UIデザイン指針に沿ったスタイリング調整、アニメーション/振動フィードバックの追加。
2. テスト（Unit / Instrumentation）とQAチェックリスト作成。
3. デモ用APKビルドと利用手順書の整備。
