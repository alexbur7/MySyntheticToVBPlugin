package com.alexbur.synthetic_plugin.visitor

import com.alexbur.synthetic_plugin.model.RootViewRef
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.idea.references.SyntheticPropertyAccessorReferenceDescriptorImpl
import org.jetbrains.kotlin.idea.structuralsearch.visitor.KotlinRecursiveElementVisitor
import org.jetbrains.kotlin.psi.KtReferenceExpression

class RootViewVisitor : KotlinRecursiveElementVisitor() {

    private val result = mutableListOf<RootViewRef>()

    fun getResult(): List<RootViewRef> = result.toList()

    override fun visitReferenceExpression(expression: KtReferenceExpression) {
        super.visitReferenceExpression(expression)
        result.addAll(expression.references.findSyntheticRefs())
    }

    private fun Array<PsiReference>.findSyntheticRefs(): List<RootViewRef> {
        return this.filterIsInstance<SyntheticPropertyAccessorReferenceDescriptorImpl>()
            .flatMap {
                //Вот тут можно получить данные по методам, например initParallaxLayout(R.layout.) строка тут есть
                it.element.references.toList()
            }
            .filter {
                it.element.text == ROOT_VIEW
            }
            .map {
                RootViewRef(it.element)
            }
    }

    private companion object {
        const val ROOT_VIEW = "rootView"
    }
}

class DotVisitor: PsiRecursiveElementWalkingVisitor(){

    private val result = mutableListOf<PsiElement>()
    private val allElement = mutableListOf<PsiElement>()

    fun getResult(): List<PsiElement> = result.toList()

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)

        if (element.text == "." && allElement.lastOrNull()?.text == "rootView") {
            result.add(element)
        }else{
            allElement.add(element)
        }
    }
}