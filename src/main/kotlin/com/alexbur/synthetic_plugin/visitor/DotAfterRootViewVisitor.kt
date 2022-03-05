package com.alexbur.synthetic_plugin.visitor

import com.alexbur.synthetic_plugin.model.TypeInitVbRef
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class DotAfterRootViewVisitor : PsiRecursiveElementWalkingVisitor() {

    var onDestroyViewPsiElement: PsiElement? = null
    private var positionRootView = 0
    private val parentResult = mutableListOf<PsiElement>()
    private val result = mutableListOf<PsiElement>()
    private val allElement = mutableListOf<PsiElement>()
    private val typeInitVBResult = mutableListOf<TypeInitVbRef>()
    private val applyReplaceWithList = mutableListOf<PsiElement>()
    private var isAdditionalBinding = false
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
    fun getTypeInitVBResults(): List<TypeInitVbRef> = typeInitVBResult.toList()
    fun getApplyReplaceWithResults(): List<PsiElement> = applyReplaceWithList.toList()

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)
        if (element is KtNameReferenceExpression) {
            if (isAdditionalBinding) {
                result.add(element)
            }
            if (typeInitVBResult.isNotEmpty() && typeInitVBResult.last().layoutId == null && element.text != "R" && element.text != "layout") {
                val typeInitVB = typeInitVBResult.last()
                typeInitVBResult.removeAt(typeInitVBResult.lastIndex)
                typeInitVBResult.add(typeInitVB.copy(layoutId = element.text))
            }
            if (TypeInitVB.values().any {
                    it.namedFunction == element.text
                }) {
                isAdditionalBinding = true
                result.add(element)
                typeInitVBResult.add(TypeInitVbRef(TypeInitVB.elementTextToType(element.text)))
            }
            if (element.text == "onDestroyView") {
                onDestroyViewPsiElement = element
            }
        }
        if (element.text == "activeLabelsAdapter") {
            println("tut")
        }
        if (element.text == "." && allElement.lastOrNull()?.text == "rootView") {
            result.add(element)
            result.add(allElement.last { it is KtNameReferenceExpression })
            positionRootView = allElement.size
        } else if (parentNames.any { it == element.text } && element is KtNameReferenceExpression) {
            parentResult.add(element)
        } else {
            if (result.lastOrNull()?.text == "rootView" && allElement.size - positionRootView < 4
                && element.text == "apply" && element is KtNameReferenceExpression
            ) {
                //result.add(element)
                applyReplaceWithList.add(element)
            } /*else if (result.lastOrNull()?.text == "apply" && element.text == "{") {
                result.add(element)
            }*/
            allElement.add(element)
        }
        if (isAdditionalBinding) {
            if (element.text == "." || element.text == "(" || element.text == ")") {
                result.add(element)
            }
            if (element.text == ")") {
                isAdditionalBinding = false
            }
        }
    }

    enum class TypeInitVB(val namedFunction: String, val nameProperty: String, val nameFunction: String) {
        INIT_FIXED_BOTTOM(
            "initFixedBottomLayout",
            "fixedBottomBinding",
            "createFixedBottomView"
        ),
        INIT_FLOAT_BOTTOM(
            "initFloatBottomLayout",
            "floatBottomBinding",
            "createFloatBottomView"
        ),
        INIT_TOP_SCROLLING(
            "initTopScrollingLayout",
            "topScrollingBinding",
            "createTopScrolledView"
        ),
        INIT_PARALLAX(
            "initParallaxLayout",
            "parallaxBinding",
            "createParallaxView"
        ),
        INIT_TOP_FIXED("initTopFixedLayout", "topFixedBinding", "createTopFixedView");

        companion object {
            fun elementTextToType(text: String): TypeInitVB {
                return when (text) {
                    INIT_FIXED_BOTTOM.namedFunction -> INIT_FIXED_BOTTOM
                    INIT_FLOAT_BOTTOM.namedFunction -> INIT_FLOAT_BOTTOM
                    INIT_TOP_FIXED.namedFunction -> INIT_TOP_FIXED
                    INIT_PARALLAX.namedFunction -> INIT_PARALLAX
                    INIT_TOP_SCROLLING.namedFunction -> INIT_TOP_SCROLLING
                    else -> throw NoSuchElementException("PsiElement text is not convert to TypeInitVB")
                }
            }
        }

    }
}