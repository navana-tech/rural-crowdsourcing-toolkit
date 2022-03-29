package com.microsoft.research.karya.data.manager

import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec

@DeleteTable.Entries(
    DeleteTable(tableName = "payout_info"),
    DeleteTable(tableName = "payout_method"),
    DeleteTable(tableName = "scenario"),
    DeleteTable(tableName = "payment_request"),
    DeleteTable(tableName = "policy"),
)
class V2ToV3Migration: AutoMigrationSpec
