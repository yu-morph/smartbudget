package com.yu_morph.smartbudget.ui.auth

internal object SignUpValidation {
    private val UsernamePattern = Regex("^[A-Za-z0-9]{4,12}$")
    private val PasswordPattern = Regex("^[A-Za-z0-9!@#\\$%^&*_=+?\\-]{8,20}$")
    private val LetterPattern = Regex("[A-Za-z]")
    private val DigitPattern = Regex("[0-9]")
    private val SpecialCharacterPattern = Regex("[!@#\\$%^&*_=+?\\-]")

    fun isValid(username: String, password: String, passwordConfirmation: String): Boolean =
        isValidUsername(username) &&
            isValidPassword(password) &&
            password == passwordConfirmation

    fun isValidUsername(username: String): Boolean = UsernamePattern.matches(username)

    fun isValidPassword(password: String): Boolean {
        if (!PasswordPattern.matches(password)) return false
        val categoryCount =
            listOf(
                LetterPattern.containsMatchIn(password),
                DigitPattern.containsMatchIn(password),
                SpecialCharacterPattern.containsMatchIn(password),
            ).count { it }
        return categoryCount >= 2
    }
}
