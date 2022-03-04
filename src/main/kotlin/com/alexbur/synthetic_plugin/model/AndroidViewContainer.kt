package com.alexbur.synthetic_plugin.model

import android.databinding.tool.ext.toCamelCase
import com.intellij.psi.PsiReference
import com.intellij.psi.xml.XmlAttributeValue
import org.jetbrains.kotlin.psi.KtReferenceExpression
import com.alexbur.synthetic_plugin.utils.Const
import java.util.*

/**
 * Model for holding information about XML view.
 */
sealed class AndroidViewContainer {

    abstract val xml: XmlAttributeValue
    abstract val isNeedBindingPrefix: Boolean

    data class PsiRef(
        val ref: PsiReference,
        override val xml: XmlAttributeValue,
        override val isNeedBindingPrefix: Boolean,
    ) : AndroidViewContainer()

    data class KtRefExp(
        val ref: KtReferenceExpression,
        override val xml: XmlAttributeValue,
        override val isNeedBindingPrefix: Boolean,
    ) : AndroidViewContainer()


    fun getElementName(): String {
        val idCamelCase = xml.text
            .removeSurrounding("\"")
            .removePrefix(Const.ANDROID_VIEW_ID)
            .toCamelCase()
            .replaceFirstChar { it.lowercase(Locale.getDefault()) }

        return when (idCamelCase) {
            "progressBar", "errorContainer", "contentContainer", "recyclerView", "viewPager", "swipeRefreshLayout" -> {
                idCamelCase
            }
            "errorTextView", "zniErrorTextView" -> {
                "errorBinding.$idCamelCase"
            }
            else -> "contentBinding.$idCamelCase"
        }
    }
}
