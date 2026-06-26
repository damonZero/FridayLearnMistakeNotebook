package com.friday.mistakenotebook.domain.usecase.subject

import com.friday.mistakenotebook.domain.repository.SubjectRepository
import javax.inject.Inject

class DeleteSubjectUseCase @Inject constructor(
    private val subjectRepository: SubjectRepository
) {
    suspend operator fun invoke(id: Long): Boolean {
        return subjectRepository.deleteSubject(id)
    }
}
