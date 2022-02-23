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
import com.alexbur.synthetic_plugin.utils.isNeedGeneric
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
        const val FRAGMENT_GROUP_IMPORT = "com.nlmk.mcs.presentation_layer.base"
    }

    private val bindingClassName = run {
        val synthImport = file.importDirectives.filter { it.importPath?.pathStr.isKotlinSynthetic() }
            .first { it.importPath?.pathStr?.contains("base")?.not() ?: false }
        val synthImportStr = synthImport.importPath?.pathStr.orEmpty()
            .removeSuffix(".*").removeSuffix(".view")
        synthImportStr
            .drop(synthImportStr.lastIndexOf('.') + 1)
            .toCamelCase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            .plus("Binding")
    }

    private val errorBinding = run {
        val synthImport = file.importDirectives.filter { it.importPath?.pathStr.isKotlinSynthetic() }
            .mapNotNull { it.importPath?.pathStr }.first { it.contains("error") }
        if (synthImport.isEmpty()) return@run null
        val synthImportStr = synthImport.removeSuffix(".*").removeSuffix(".view")
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
            addImports("${FRAGMENT_VB_GROUP_IMPORT}.${parents.newParentFragment}")
            deleteImport("$FRAGMENT_GROUP_IMPORT.${parents.oldParentFragment}")

            if (parents.newParentFragment.isNeedGeneric()) {
                when {
                    parents.isChildOf(ANDROID_FRAGMENT_CLASS) -> processFragment(ktClass)
                    else -> println("Can't add ViewBinding property to class: ${psiClass.qualifiedName}")
                }
                addImports(bindingQualifiedClassName)
                parentClass?.replace(psiFactory.creareDelegatedSuperTypeEntry("${parents.newParentFragment}<"))
                    ?.add(psiFactory.creareDelegatedSuperTypeEntry("${bindingClassName}>"))
            } else {
                parentClass?.replace(psiFactory.creareDelegatedSuperTypeEntry("${parents.newParentFragment}"))
            }
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
        body.addAfter(psiFactory.createNewLine(), body.lBrace)
        if (file.importDirectives.find { it.importPath?.pathStr == VIEW_GROUP_IMPORT } == null) {
            addImports(VIEW_GROUP_IMPORT)
        } else if (file.importDirectives.find { it.importPath?.pathStr == LAYOUT_INFLATER_GROUP_IMPORT } == null) {
            addImports(LAYOUT_INFLATER_GROUP_IMPORT)
        }
        val binding = errorBinding ?: return
        addImports("com.nlmk.mcs.databinding.${binding}")
        val createErrorBindingText = "override fun createErrorView(\n" +
                "        inflater: LayoutInflater,\n" +
                "        container: ViewGroup?,\n" +
                "        isAttach: Boolean\n" +
                "    ): View? {\n" +
                "        errorBinding = $binding.inflate(inflater, container, isAttach)\n" +
                "        return errorBinding?.root\n" +
                "    }"
        val errorBindingMethod = psiFactory.createFunction(createErrorBindingText)
        body.addAfter(errorBindingMethod, body.lBrace)
        body.addAfter(psiFactory.createNewLine(), body.lBrace)
        val errorText = "private var errorBinding: $binding? = null"
        val errorBindingProperty = psiFactory.createProperty(errorText)
        body.addAfter(errorBindingProperty, body.lBrace)
        body.addAfter(psiFactory.createNewLine(), body.lBrace)
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

    private fun deleteImport(vararg imports: String) {
        file.importList?.let { importList ->
            imports.forEach { import ->
                val importPath = ImportPath.fromString(import)
                val a = importList.imports.filter {
                    it.importPath == importPath
                }
                a.forEach {
                    it.delete()
                }
            }
        }
    }
}
