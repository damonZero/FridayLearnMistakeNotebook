package com.friday.mistakenotebook.domain.usecase

import com.friday.mistakenotebook.domain.repository.SubjectRepository
import com.friday.mistakenotebook.domain.usecase.subject.AddSubjectUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class AddSubjectUseCaseTest {

    private lateinit var subjectRepository: SubjectRepository
    private lateinit var addSubjectUseCase: AddSubjectUseCase

    @Before
    fun setup() {
        subjectRepository = mock()
        addSubjectUseCase = AddSubjectUseCase(subjectRepository)
    }

    @Test
    fun `invoke should call repository addSubject`() = runTest {
        whenever(subjectRepository.addSubject(any(), any(), any())).thenReturn(1L)

        val result = addSubjectUseCase("数学", "🔢", "#2196F3")

        assertEquals(1L, result)
        verify(subjectRepository).addSubject("数学", "🔢", "#2196F3")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke should throw exception for blank name`() = runTest {
        addSubjectUseCase("", "🔢", "#2196F3")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke should throw exception for whitespace name`() = runTest {
        addSubjectUseCase("   ", "🔢", "#2196F3")
    }

    @Test
    fun `invoke should pass correct parameters`() = runTest {
        whenever(subjectRepository.addSubject(any(), any(), any())).thenReturn(1L)

        addSubjectUseCase("英语", "🔤", "#4CAF50")

        argumentCaptor<String>().apply {
            verify(subjectRepository).addSubject(capture(), capture(), capture())
            assertEquals("英语", firstValue)
            assertEquals("🔤", secondValue)
            assertEquals("#4CAF50", thirdValue)
        }
    }
}
