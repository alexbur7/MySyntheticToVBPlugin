package com.alexbur.synthetic_plugin.delegates

import android.databinding.tool.ext.toCamelCase
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.resolve.ImportPath
import com.alexbur.synthetic_plugin.utils.ClassParentsFinder
import com.alexbur.synthetic_plugin.extensions.isKotlinSynthetic
import com.alexbur.synthetic_plugin.model.TypeInitVbRef
import com.alexbur.synthetic_plugin.utils.isNeedGeneric
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*
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
        val synthImport = file.importDirectives.filter {
            it.importPath?.pathStr.isKotlinSynthetic()
        }.mapNotNull {
            it.importPath?.pathStr
        }
        if (synthImport.isEmpty()) return@run null
        val synthImport1 = synthImport.firstOrNull {
            it.contains("base").not()
        }
        if (synthImport1.isNullOrEmpty()) return@run null
        val synthImportStr = synthImport1
            .removeSuffix(".*").removeSuffix(".view")
        synthImportStr
            .drop(synthImportStr.lastIndexOf('.') + 1)
            .toCamelCase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            .plus("Binding")
    }

    private val errorBinding = run {
        val synthImport = file.importDirectives.filter {
            it.importPath?.pathStr.isKotlinSynthetic()
        }
        if (synthImport.isEmpty()) return@run null
        val synthImport1 = synthImport
            .mapNotNull { it.importPath?.pathStr }.firstOrNull { it.contains("_error_") }
        if (synthImport1.isNullOrEmpty()) return@run null
        val synthImportStr = synthImport1.removeSuffix(".*").removeSuffix(".view")
        synthImportStr
            .drop(synthImportStr.lastIndexOf('.') + 1)
            .toCamelCase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            .plus("Binding")
    }

    private val bindingQualifiedClassName = run {
        "com.nlmk.mcs.databinding.${bindingClassName}"
    }
    private val cleanBindingText = mutableListOf<String>()

    fun addViewBindingProperty(
        parentClass: PsiElement?,
        typeInitVBResult: List<TypeInitVbRef>,
        onDestroyViewPsiElement: PsiElement?
    ) {
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

            createErrorBinding(ktClass)
            setAdditionalBindings(typeInitVBResult, ktClass)
            clearBindingsInDestroyView(onDestroyViewPsiElement, ktClass)
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

    private fun createErrorBinding(ktClass: KtClass) {
        val binding = errorBinding ?: return
        val body = ktClass.getOrCreateBody()
        addImports("com.nlmk.mcs.databinding.${binding}")
        val createErrorBindingText = "override fun createErrorView(\n" +
                "        inflater: LayoutInflater,\n" +
                "        container: ViewGroup?,\n" +
                "        isAttach: Boolean\n" +
                "    ): View? {\n" +
                "        _errorBinding = $binding.inflate(inflater, container, isAttach)\n" +
                "        return _errorBinding?.root\n" +
                "    }"
        val errorBindingMethod = psiFactory.createFunction(createErrorBindingText)
        body.addAfter(errorBindingMethod, body.lBrace)
        body.addAfter(psiFactory.createNewLine(), body.lBrace)
        val notNullErrorBindingText = "private val errorBinding: $binding\n" +
                "get() = checkNotNull(_errorBinding)"
        val notNullErrorBindingProperty = psiFactory.createProperty(notNullErrorBindingText)
        body.addAfter(notNullErrorBindingProperty, body.lBrace)
        body.addAfter(psiFactory.createNewLine(), body.lBrace)
        val errorText = "private var _errorBinding: $binding? = null"
        val errorBindingProperty = psiFactory.createProperty(errorText)
        body.addAfter(errorBindingProperty, body.lBrace)
        body.addAfter(psiFactory.createNewLine(), body.lBrace)
        setViewGroupImport()
        cleanBindingText.add("_errorBinding = null")
    }

    private fun setAdditionalBindings(
        typeInitVBResult: List<TypeInitVbRef>,
        ktClass: KtClass
    ) {
        if (typeInitVBResult.isEmpty()) return
        val body = ktClass.getOrCreateBody()
        typeInitVBResult.forEach { typeInitVbRef ->
            val initBinding = typeInitVbRef.layoutId?.toCamelCase()
                ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                .plus("Binding")
            addImports("com.nlmk.mcs.databinding.${initBinding}")
            val initBindingText = "override fun ${typeInitVbRef.typeInitVB.nameFunction}(\n" +
                    "        inflater: LayoutInflater,\n" +
                    "        container: ViewGroup?,\n" +
                    "        isAttach: Boolean\n" +
                    "    ): View? {\n" +
                    "        _${typeInitVbRef.typeInitVB.nameProperty} = $initBinding.inflate(inflater, container, isAttach)\n" +
                    "        return _${typeInitVbRef.typeInitVB.nameProperty}?.root\n" +
                    "    }"
            val initMethod = psiFactory.createFunction(initBindingText)
            body.addAfter(initMethod, body.lBrace)
            body.addAfter(psiFactory.createNewLine(), body.lBrace)
            val notNullInitTextBinding = "private val ${typeInitVbRef.typeInitVB.nameProperty}: $initBinding\n" +
                    "get() = checkNotNull(_${typeInitVbRef.typeInitVB.nameProperty})"
            val notNullInitTextProperty = psiFactory.createProperty(notNullInitTextBinding)
            body.addAfter(notNullInitTextProperty, body.lBrace)
            body.addAfter(psiFactory.createNewLine(), body.lBrace)
            val initTextBinding = "private var _${typeInitVbRef.typeInitVB.nameProperty}: $initBinding? = null"
            val initTextProperty = psiFactory.createProperty(initTextBinding)
            body.addAfter(initTextProperty, body.lBrace)
            body.addAfter(psiFactory.createNewLine(), body.lBrace)
            cleanBindingText.add("_${typeInitVbRef.typeInitVB.nameProperty} = null")
        }
        setViewGroupImport()
    }

    private fun clearBindingsInDestroyView(
        onDestroyViewPsiElement: PsiElement?,
        ktClass: KtClass
    ) {
        if (cleanBindingText.isEmpty()) return
        val body = ktClass.getOrCreateBody()
        if (onDestroyViewPsiElement == null) {
            val functionText = "override fun onDestroyView() {\n" +
                    cleanBindingText.joinToString("\n") +
                    "\n"+
                    "super.onDestroyView()\n" +
                    "}"
            body.addAfter(
                psiFactory.createFunction(functionText),
                body.lBrace
            )
        } else {
            onDestroyViewPsiElement.add(psiFactory.createNewLine())
            cleanBindingText.forEach { text ->
                onDestroyViewPsiElement.add(psiFactory.createArgument(text)).add(psiFactory.createNewLine())
            }
        }
    }

    private fun setViewGroupImport() {
        if (file.importDirectives.find { it.importPath?.pathStr == VIEW_GROUP_IMPORT } == null) {
            addImports(VIEW_GROUP_IMPORT)
        }
        if (file.importDirectives.find { it.importPath?.pathStr == LAYOUT_INFLATER_GROUP_IMPORT } == null) {
            addImports(LAYOUT_INFLATER_GROUP_IMPORT)
        }
    }

    //для создания биндинга объекта
    private fun processFragment(ktClass: KtClass) {
        val text =
            "override val contentBindingInflate: ((LayoutInflater, ViewGroup?, Boolean) -> $bindingClassName)\n" +
                    "        get() = $bindingClassName::inflate"
        val viewBindingDeclaration = psiFactory.createProperty(text)
        val body = ktClass.getOrCreateBody()
        setViewGroupImport()

        // It would be nice to place generated property after companion objects inside Fragments
        // and Views (if we have one). Also we should add [newLine] before generated property declaration.
        body.addAfter(viewBindingDeclaration, body.lBrace)
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
