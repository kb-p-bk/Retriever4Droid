

100% on-device Android application for document parsing, configurable text chunking, vector embedding, and semantic vector search; completely offline with zero server dependency.

---

## Key Features

* **Multi-Format Document Parsing**: Supports `PDF`, `EPUB`, `MOBI`, `DOCX`, `TXT`, and `Markdown`.
* **Configurable Text Chunking**:
  * Chunking Strategies: **Recursive** (Sentence/Paragraph boundaries), **Paragraph**, and **Chapter** splitting.
  * Customizable **Chunk Size** (128 – 2048 characters) and **Chunk Overlap** (0 – 200 characters).
* **On-Device AI Embeddings & Vector Search**:
  * Powered by **ONNX Runtime** using `all-MiniLM-L6-v2` (384-dimensional float vector embeddings).
  * High-performance **HNSW Indexing** via **ObjectBox** vector database.
  * Configurable Top-K search results count (5 to 100 results).
  * In-line full text expansion on search result cards + interactive **Chunk Viewer** screen.
* **Flexible Search Filtering**:
  * Multi-select document filter with inner name search.
  * Real-time tag filter chips synced across the Library and Search tabs.
* **Custom ONNX Model Support**:
  * Download and activate any 384D Hugging Face ONNX sentence-transformer model (presets for `all-MiniLM-L6-v2`, `bge-small-en-v1.5`, `gte-small`).
  * Load local `.onnx` model files directly from tablet/phone storage.
* **Direct Document Tagging**:
  * Add tags immediately upon completing ingestion or edit tags anytime in the Document Library.
* **Responsive Tablet & Dark Mode UX**:
  * **Adaptive Layout**: Optimized for 12.4" tablets (such as Samsung Galaxy Tab S9 FE+) with a side `NavigationRail` in tablet mode and `NavigationBar` in phone mode.
  * **System Dark Theme**: Complete Material 3 dark theme with edge-to-edge status bar inset padding.
  * **Live Notifications**: Foreground system notifications showing live progress while ingesting in the background.

---

## Tech Stack

* **Language & Framework**: [Kotlin](https://kotlinlang.org/) · [Jetpack Compose](https://developer.android.com/jetpack/compose) · Material 3 · [Hilt DI](https://dagger.dev/hilt/)
* **Background Tasks**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) + Android Notifications
* **AI & Embeddings Engine**: [ONNX Runtime Android](https://onnxruntime.ai/) (`onnxruntime-android:1.19.2`)
* **Vector Database**: [ObjectBox Android](https://objectbox.io/) (`io.objectbox:4.0.3`) with HNSW index
* **Document Parsers**:
  * `PDF`: [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android) (`2.0.27.0`)
  * `EPUB`: [Epublib Core](https://github.com/psiegman/epublib) (`com.positiondev.epublib:3.1`)
  * `DOCX`: [Apache POI OOXML](https://poi.apache.org/) (`5.3.0`)
  * `HTML / Cleaning`: [jsoup](https://jsoup.org/) (`1.18.1`)

---

## Architecture & Module Structure

The project follows **Clean Architecture** principles structured into 14 Gradle modules across 4 layers:

```text
Retriever4Droid/
├── app/                  # Main application module (Hilt setup, Navigation Host, Settings)
├── core/
│   ├── common/           # Coroutine Dispatchers, Result wrappers, Extensions
│   ├── model/            # Domain models (Document, TextChunk, ChunkConfig, SearchResult)
│   └── ui/               # Material 3 theme (Color, Theme, Typography) & shared components
├── domain/               # Core business logic: UseCases, Repository interfaces, Service ports
├── data/
│   ├── parser/           # Document parsers (Pdf, Epub, Mobi, Docx, Txt)
│   ├── chunking/         # Text splitters (Recursive, Paragraph, Chapter)
│   ├── embedding/        # ONNX ModelManager, TokenizerWrapper, OnnxEmbeddingService
│   ├── vectorstore/      # ObjectBox vector database, ChunkEntity, DocumentEntity
│   ├── file/             # Storage Access Framework (SAF) file repository
│   └── repository/       # Data repository implementations & SettingsRepository
└── feature/
    ├── home/             # Home Dashboard screen & stats
    ├── ingest/           # Document Ingestion UI, Vertical Stepper, Tagging, IngestWorker
    ├── search/           # Vector Search UI, Filter Bar, ChunkViewerScreen
    └── library/          # Document Library UI, Storage Summary, Tag Editor
```

---

## Build Requirements & Prerequisites

Before building the project, ensure your environment meets the following requirements:

1. **JDK**: **Java 17** or higher (`JavaVersion.VERSION_17`, `jvmTarget = "17"`).
2. **Android SDK**:
   * `compileSdk`: **35**
   * `minSdk`: **26** (Android 8.0 Oreo or higher)
   * `targetSdk`: **35**
3. **Gradle**: **Gradle 8.9** (configured in `gradle/wrapper/gradle-wrapper.properties`).
4. **Gradle JVM Memory**: Requires at least `-Xmx4096m` heap allocation (configured in `gradle.properties`) for Hilt, KSP, and ObjectBox code generation.

---

## Model & Asset Requirements

The application requires the ONNX model and tokenizer vocabulary files inside `app/src/main/assets/models/`:

* `app/src/main/assets/models/all-MiniLM-L6-v2.onnx` (~23MB INT8 quantized ONNX model)
* `app/src/main/assets/models/vocab.json` (WordPiece 30,522 token vocabulary)

> **Note**: These files are pre-packaged in the project assets. On first app launch, `ModelManager` automatically extracts `all-MiniLM-L6-v2.onnx` to the app's internal files directory (`context.filesDir/models/`).

---

## Building & Running

### 1. Build Debug APK via Command Line
Run the Gradle wrapper assemble command:

```bash
# On Linux / macOS
./gradlew :app:assembleDebug

# On Windows (PowerShell / CMD)
.\gradlew.bat :app:assembleDebug
```

The generated APK will be placed at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

### 2. Install APK onto Device or Emulator via ADB

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Troubleshooting & Common Build Issues

1. **`Minimum supported Gradle version is 8.9`**:
   Ensure `gradle/wrapper/gradle-wrapper.properties` contains:
   `distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip`

2. **`Out of memory: Java heap space`**:
   Increase Gradle daemon memory in `gradle.properties`:
   `org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8`

3. **`ObjectBox Configuration Cache issue`**:
   The ObjectBox 4.0.3 Gradle plugin requires configuration cache to be disabled in `gradle.properties`:
   `org.gradle.configuration-cache=false`

4. **`Duplicate class org.xmlpull.v1.XmlPullParser`**:
   Transitive dependencies from POI or Epublib must exclude `xmlpull:xmlpull` in favour of the Android Framework XML parser. This is configured in `data/parser/build.gradle.kts`.

---

* ONNX Embedding model: [sentence-transformers/all-MiniLM-L6-v2](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2)
* Vector Database Engine: [ObjectBox Java/Kotlin](https://github.com/objectbox/objectbox-java)

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

### Third-Party Components

This application bundles the **all-MiniLM-L6-v2** sentence embedding model:
- **Source**: [sentence-transformers/all-MiniLM-L6-v2](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2)
- **License**: Apache License 2.0
- **Copyright**: 2019 Nils Reimers
- **Full License**: https://www.apache.org/licenses/LICENSE-2.0

The Apache 2.0 licensed model is compatible with this project's MIT license and allows commercial use, modification, and distribution.
