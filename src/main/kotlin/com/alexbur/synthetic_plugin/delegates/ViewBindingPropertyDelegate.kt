package com.alexbur.synthetic_plugin.delegates

import android.databinding.tool.ext.toCamelCase
import com.intellij.psi.PsiClass
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.getOrCreateBody
import org.jetbrains.kotlin.resolve.ImportPath
import com.alexbur.synthetic_plugin.utils.ClassParentsFinder
import com.alexbur.synthetic_plugin.extensions.getPackageName
import com.alexbur.synthetic_plugin.extensions.isKotlinSynthetic
import java.util.*

/**
 * TODO:
 *
 * - [ ] It would be nice to place generated property after companion objects inside Fragments
 * and Views (if we have one). Also we should add [newLine] before generated property declaration.
 *
 * - [ ] For Cells we should handle case when there is no `with(viewHolder.itemView)` block
 * (case with direct invocation of `viewHolder.itemView.view_id`)
 */
class ViewBindingPropertyDelegate(
    private val psiFactory: KtPsiFactory,
    private val file: KtFile,
    androidFacet: AndroidFacet?,
) {

    private companion object {
        const val ANDROID_FRAGMENT_CLASS = "androidx.fragment.app.Fragment"
        const val VIEW_GROUP_IMPORT = "android.view.ViewGroup"
        const val LAYOUT_INFLATER_GROUP_IMPORT = "android.view.LayoutInflater"
        const val FRAGMENT_VB_GROUP_IMPORT = "com.nlmk.mcs.presentation_layer.base.vb"
    }

    private val bindingClassName = run {
        val synthImport = file.importDirectives.first { it.importPath?.pathStr.isKotlinSynthetic() }
        val synthImportStr = synthImport.importPath?.pathStr.orEmpty()
            .removeSuffix(".*").removeSuffix(".view")
        synthImportStr
            .drop(synthImportStr.lastIndexOf('.') + 1)
            .toCamelCase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            .plus("Binding")
    }

    private val bindingQualifiedClassName = run {
        val packageName = "${androidFacet?.getPackageName().orEmpty()}.databinding."
        "${packageName}${bindingClassName}"
    }

    fun addViewBindingProperty() {
        // `as Array<PsiClass` is necessary because of MISSING_DEPENDENCY_CLASS error from Kotlin Gradle plugin
        // https://youtrack.jetbrains.com/issue/KTIJ-19485
        // https://youtrack.jetbrains.com/issue/KTIJ-10861
        val classes = (file.classes as Array<PsiClass>).mapNotNull { psiClass ->
            val ktClass = ((psiClass as? KtLightElement<*, *>)?.kotlinOrigin as? KtClass)
            if (ktClass == null) {
                null
            } else {
                psiClass to ktClass
            }
        }
        classes.forEach { (psiClass, ktClass) ->
            val parents = ClassParentsFinder(psiClass)
            when {
                parents.isChildOf(ANDROID_FRAGMENT_CLASS) -> processFragment(ktClass)
                else -> println("Can't add ViewBinding property to class: ${psiClass.qualifiedName}")
            }
            //parents.changeParentName(psiFactory, bindingClassName)
            addImports(bindingQualifiedClassName)
            addImports("${FRAGMENT_VB_GROUP_IMPORT}.${parents.typeVb?.newName}")
        }
    }

    //для создания биндинга объекта
    private fun processFragment(ktClass: KtClass) {
        val text =
            "override val contentBindingInflate: ((LayoutInflater, ViewGroup?, Boolean) -> $bindingClassName)\n" +
                    "        get() = $bindingClassName::inflate"
        val viewBindingDeclaration = psiFactory.createProperty(text)
        val body = ktClass.getOrCreateBody()

        // It would be nice to place generated property after companion objects inside Fragments
        // and Views (if we have one). Also we should add [newLine] before generated property declaration.
        body.addAfter(viewBindingDeclaration, body.lBrace)
        addImports(VIEW_GROUP_IMPORT, LAYOUT_INFLATER_GROUP_IMPORT)
    }

    private fun addImports(vararg imports: String) {
        file.importList?.let { importList ->
            imports.forEach { import ->
                val importPath = ImportPath.fromString(import)
                val importDirective = psiFactory.createImportDirective(importPath)
                importList.add(psiFactory.createNewLine())
                importList.add(importDirective)
            }
        }
    }
}
