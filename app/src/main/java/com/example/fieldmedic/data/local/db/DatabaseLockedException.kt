package com.example.fieldmedic.data.local.db

class DatabaseLockedException(message: String = "Database is locked") : IllegalStateException(message)
