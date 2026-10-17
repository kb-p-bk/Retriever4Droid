# ProGuard rules for Retriever4Droid

# Keep ONNX Runtime classes
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# Keep ObjectBox entities and generated code
-keep class com.ragdroid.data.vectorstore.entity.** { *; }
-keep class io.objectbox.** { *; }
-dontwarn io.objectbox.**

# Keep Apache POI (required for DOCX parsing)
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.commons.**
-dontwarn org.apache.xmlbeans.**
-dontwarn javax.xml.**

# Keep PdfBox-Android
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**

# Keep Epublib
-keep class nl.siegmann.epublib.** { *; }
-dontwarn nl.siegmann.epublib.**

# Keep LangChain4j
-keep class dev.langchain4j.** { *; }
-dontwarn dev.langchain4j.**
