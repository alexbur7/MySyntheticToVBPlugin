package com.alexbur.synthetic_plugin.extensions

import com.alexbur.synthetic_plugin.utils.Const

fun String?.isKotlinSynthetic(): Boolean {
    return this?.startsWith(Const.KOTLINX_SYNTHETIC) == true
}