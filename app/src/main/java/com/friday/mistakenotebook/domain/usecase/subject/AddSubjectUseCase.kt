package com.friday.mistakenotebook.domain.usecase.subject

import com.friday.mistakenotebook.domain.repository.SubjectRepository
import javax.inject.Inject

class AddSubjectUseCase @Inject constructor(
    private val subjectRepository: SubjectRepository
) {
    suspend operator fun invoke(name: String, icon: String, color: String): Long {
        require(name.isNotBlank()) { "科目名称不能为空" }
        return subjectRepository.addSubject(name, icon, color)
    }
}
