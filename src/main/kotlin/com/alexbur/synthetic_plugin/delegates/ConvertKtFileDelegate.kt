package com.alexbur.synthetic_plugin.delegates

import com.intellij.openapi.project.Project
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiFactory
import com.alexbur.synthetic_plugin.model.AndroidViewContainer
import com.alexbur.synthetic_plugin.model.RootViewRef
import com.alexbur.synthetic_plugin.visitor.AndroidViewXmlSyntheticsRefsVisitor
import com.alexbur.synthetic_plugin.visitor.DotVisitor
import com.alexbur.synthetic_plugin.visitor.RootViewVisitor
import com.alexbur.synthetic_plugin.visitor.SyntheticsImportsVisitor
import com.intellij.psi.PsiElement

object ConvertKtFileDelegate {

    /**
     * This function should be invoked inside [com.intellij.openapi.project.Project.executeWriteCommand]
     * because this method modify your codebase.
     */
    fun perform(
        file: KtFile,
        project: Project,
        androidFacet: AndroidFacet?,
        psiFactory: KtPsiFactory = KtPsiFactory(project),
    ) {
        val xmlRefsVisitor = AndroidViewXmlSyntheticsRefsVisitor()
        val importsVisitor = SyntheticsImportsVisitor()
        val rootViewVisitor = RootViewVisitor()
        val dotVisitor = DotVisitor()
        file.accept(xmlRefsVisitor)
        file.accept(importsVisitor)
        file.accept(rootViewVisitor)
        file.accept(dotVisitor)
        val xmlViewRefs = xmlRefsVisitor.getResult()
        val syntheticImports = importsVisitor.getResult()
        val rootViewRefs = rootViewVisitor.getResult()
        val dots = dotVisitor.getResult()

        ViewBindingPropertyDelegate(psiFactory, file, androidFacet).addViewBindingProperty()
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
