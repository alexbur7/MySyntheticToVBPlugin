package com.alexbur.synthetic_plugin.visitor

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class DotAfterRootViewVisitor : PsiRecursiveElementWalkingVisitor() {

    private val parentResult = mutableListOf<PsiElement>()
    private val result = mutableListOf<PsiElement>()
    private val allElement = mutableListOf<PsiElement>()
    private var position = 0
    private val parentNames = listOf(
        "BaseFragment",
        "CollapsingTitleFragment",
        "BaseRefreshFragment",
        "CenterTitleFragment",
        "LargeTitleFragment",
        "LiftableTitleFragment",
        "CollapsingTitleRecyclerFragment",
        "CollapsingTitleRefreshRecyclerFragment",
        "CollapsingTitleRefreshRecyclerWithButtonFragment",
        "CollapsingTitleViewPagerFragment"
    )

    fun getResult(): List<PsiElement> = result.toList()
    fun getParentResult(): PsiElement? = parentResult.lastOrNull()

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)

        if (element.text == "." && allElement.lastOrNull()?.text == "rootView") {
            result.add(element)
        } else if (parentNames
                .any { it == element.text } && element is KtNameReferenceExpression
        ) {
            parentResult.add(element)
            position = allElement.size
        } else {
            allElement.add(element)
        }
    }
}