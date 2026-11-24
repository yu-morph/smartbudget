package com.yu_morph.smartbudget.ui.main

import com.yu_morph.smartbudget.R
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountSaveMessageTest {
    @Test
    fun `계정 설정 종류별 성공 메시지를 구분한다`() {
        assertEquals(R.string.settings_display_name_updated, accountSaveMessage(false, true, false))
        assertEquals(R.string.settings_budget_saved, accountSaveMessage(false, false, true))
        assertEquals(R.string.settings_account_settings_saved, accountSaveMessage(false, true, true))
        assertEquals(R.string.settings_password_updated, accountSaveMessage(true, true, false))
    }
}
