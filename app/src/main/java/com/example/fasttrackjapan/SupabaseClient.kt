package com.example.fasttrackjapan

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.storage.Storage

object Supabase {
    val client = createSupabaseClient(
        supabaseUrl = "https://jbdgsloifqikoeukjkup.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpiZGdzbG9pZnFpa29ldWtqa3VwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIyNjgyMTMsImV4cCI6MjA5Nzg0NDIxM30.liShXpAFggPEnpCpQOCe-W890Vp_m0XhkktkFReuCr8"
    ) {
        install(Postgrest)
        install(Auth) {
            alwaysAutoRefresh = true
        }
        install(Storage)
    }
}
