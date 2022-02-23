package com.alexbur.synthetic_plugin.delegates

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiFactory
import com.alexbur.synthetic_plugin.model.AndroidViewContainer
import com.alexbur.synthetic_plugin.model.RootViewRef
import com.alexbur.synthetic_plugin.visitor.AndroidViewXmlSyntheticsRefsVisitor
import com.alexbur.synthetic_plugin.visitor.DotAfterRootViewVisitor
import com.alexbur.synthetic_plugin.visitor.RootViewVisitor
import com.alexbur.synthetic_plugin.visitor.SyntheticsImportsVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager

object ConvertKtFileDelegate {

    fun perform(
        file: KtFile,
        project: Project,
        psiFactory: KtPsiFactory = KtPsiFactory(project),
    ) {
        val xmlRefsVisitor = AndroidViewXmlSyntheticsRefsVisitor()
        val importsVisitor = SyntheticsImportsVisitor()
        val rootViewVisitor = RootViewVisitor()
        val dotAfterRootViewVisitor = DotAfterRootViewVisitor()
        file.accept(xmlRefsVisitor)
        file.accept(importsVisitor)
        file.accept(rootViewVisitor)
        file.accept(dotAfterRootViewVisitor)
        val xmlViewRefs = xmlRefsVisitor.getResult()
        val syntheticImports = importsVisitor.getResult()
        val rootViewRefs = rootViewVisitor.getResult()
        val dots = dotAfterRootViewVisitor.getResult()
        val parents = dotAfterRootViewVisitor.getParentResult()

        val bindingPropertyDelegate = ViewBindingPropertyDelegate(psiFactory, file)
        bindingPropertyDelegate.addViewBindingProperty(parents)
        CodeStyleManager.getInstance(project).reformat(file)
        replaceSynthCallsToViews(psiFactory, xmlViewRefs)
        removeKotlinxSyntheticsImports(syntheticImports)
        removeRootView(rootViewRefs)
        removeDot(dots)

        println("Converted synthetics to view binding for ${file.name} successfully")
    }

    private fun replaceSynthCallsToViews(psiFactory: KtPsiFactory, xmlViewRefs: List<AndroidViewContainer>) {
        xmlViewRefs.forEach { refContainer ->
            val newElement = psiFactory.createArgument(refContainer.getElementName())

            when (refContainer) {
                is AndroidViewContainer.KtRefExp -> {
                    refContainer.ref.replace(newElement)
                }
                is AndroidViewContainer.PsiRef -> {
                    refContainer.ref.element.replace(newElement)
                }
            }
        }
    }

    private fun removeKotlinxSyntheticsImports(syntheticImports: List<KtImportDirective>) {
        syntheticImports.forEach { import ->
            import.delete()
        }
    }

    private fun removeRootView(rootViews: List<RootViewRef>) {
        rootViews.forEach {
            it.ref?.delete()
        }
    }

    private fun removeDot(list: List<PsiElement>) {
        list.forEach {
            it.delete()
        }
    }
}
