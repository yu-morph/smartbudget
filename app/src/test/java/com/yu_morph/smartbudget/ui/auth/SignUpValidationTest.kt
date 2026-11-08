package com.yu_morph.smartbudget.ui.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignUpValidationTest {
    @Test
    fun `유효한 사용자 이름과 비밀번호는 가입할 수 있다`() {
        assertTrue(SignUpValidation.isValid("User123", "Password1!", "Password1!"))
    }

    @Test
    fun `사용자 이름은 영문과 숫자 4자에서 12자만 허용한다`() {
        assertTrue(SignUpValidation.isValidUsername("User123"))
        assertFalse(SignUpValidation.isValidUsername("abc"))
        assertFalse(SignUpValidation.isValid("abc", "Password1!", "Password1!"))
        assertFalse(SignUpValidation.isValid("사용자1", "Password1!", "Password1!"))
        assertFalse(SignUpValidation.isValid("user_name", "Password1!", "Password1!"))
    }

    @Test
    fun `비밀번호는 허용 문자 중 두 종류 이상을 포함해야 한다`() {
        assertFalse(SignUpValidation.isValidPassword("abcdefgh"))
        assertTrue(SignUpValidation.isValidPassword("abcdef12"))
        assertFalse(SignUpValidation.isValid("User123", "abcdefgh", "abcdefgh"))
        assertFalse(SignUpValidation.isValid("User123", "Password 한글", "Password 한글"))
        assertTrue(SignUpValidation.isValid("User123", "abcdef12", "abcdef12"))
    }

    @Test
    fun `비밀번호 확인이 일치해야 한다`() {
        assertFalse(SignUpValidation.isValid("User123", "Password1!", "Password2!"))
    }
}
