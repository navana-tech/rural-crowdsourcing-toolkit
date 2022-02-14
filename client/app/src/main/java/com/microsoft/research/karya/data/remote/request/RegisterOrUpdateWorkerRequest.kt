// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.research.karya.data.remote.request

import com.google.gson.JsonElement

data class RegisterOrUpdateWorkerRequest(
  var year_of_birth: String,
  var gender: String,
  var profile: JsonElement,
)
