package com.alexbur.synthetic_plugin.delegates

import android.databinding.tool.ext.toCamelCase
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.getOrCreateBody
import org.jetbrains.kotlin.resolve.ImportPath
import com.alexbur.synthetic_plugin.utils.ClassParentsFinder
import com.alexbur.synthetic_plugin.extensions.isKotlinSynthetic
import com.intellij.psi.PsiElement
import java.util.*

class ViewBindingPropertyDelegate(
    private val psiFactory: KtPsiFactory,
    private val file: KtFile
) {

    private companion object {
        const val ANDROID_FRAGMENT_CLASS = "androidx.fragment.app.Fragment"
        const val VIEW_GROUP_IMPORT = "android.view.ViewGroup"
        const val LAYOUT_INFLATER_GROUP_IMPORT = "android.view.LayoutInflater"
        const val FRAGMENT_VB_GROUP_IMPORT = "com.nlmk.mcs.presentation_layer.base.vb"
    }

    val bindingClassName = run {
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
        "com.nlmk.mcs.databinding.${bindingClassName}"
    }

    fun addViewBindingProperty(parentClass: PsiElement?) {
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
            addImports(bindingQualifiedClassName)
            addImports("${FRAGMENT_VB_GROUP_IMPORT}.${parents.typeVb?.newName}")
            parentClass?.replace(psiFactory.creareDelegatedSuperTypeEntry("${parents.typeVb?.newName}<"))
                ?.add(psiFactory.creareDelegatedSuperTypeEntry("${bindingClassName}>"))
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
