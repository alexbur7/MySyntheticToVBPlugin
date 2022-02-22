package com.alexbur.synthetic_plugin.model

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.psi.KtReferenceExpression

data class RootViewRef(
    val ref: PsiElement?
)
