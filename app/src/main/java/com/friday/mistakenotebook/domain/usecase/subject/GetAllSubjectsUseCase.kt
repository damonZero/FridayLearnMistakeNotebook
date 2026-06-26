package com.friday.mistakenotebook.domain.usecase.subject

import com.friday.mistakenotebook.domain.model.Subject
import com.friday.mistakenotebook.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllSubjectsUseCase @Inject constructor(
    private val subjectRepository: SubjectRepository
) {
    operator fun invoke(): Flow<List<Subject>> {
        return subjectRepository.getAllSubjects()
    }
}
