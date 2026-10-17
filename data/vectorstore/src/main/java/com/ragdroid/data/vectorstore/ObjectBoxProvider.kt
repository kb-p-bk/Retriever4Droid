package com.ragdroid.data.vectorstore

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.objectbox.BoxStore
import com.ragdroid.data.vectorstore.entity.MyObjectBox
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the ObjectBox BoxStore singleton.
 * ObjectBox generates the MyObjectBox class from annotated @Entity classes.
 */
@Singleton
class ObjectBoxProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val boxStore: BoxStore by lazy {
        MyObjectBox.builder()
            .androidContext(context)
            .name("retriever4droid-vectordb")
            .build()
    }
}
