package com.abdulrahman.littlesnap

import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Size
import java.lang.Long.signum

internal class CompareSizeByArea:Comparator<Size> {
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun compare(lhs: Size, rhs: Size): Int {
      return  signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
    }
}