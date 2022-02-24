package com.alexbur.synthetic_plugin.model

import com.alexbur.synthetic_plugin.visitor.DotAfterRootViewVisitor

data class TypeInitVbRef(
    val typeInitVB: DotAfterRootViewVisitor.TypeInitVB,
    val layoutId: String? = null
)