package com.acme.foo

import dev.packageprivate.PackagePrivate

@PackagePrivate
fun ok(): String = Hidden().helper()
