import { describe, expect, it } from 'vitest'
import { validateFolderName } from '../src/modules/folder/folderValidators'
import { validateQuestion, normalizeAnswer } from '../src/modules/question/questionValidators'
import { validateArchive } from '../src/services/archiveService'

describe('folder validators', () => {
  it('rejects empty names', () => {
    expect(validateFolderName('   ').valid).toBe(false)
  })

  it('rejects duplicate names case-insensitively', () => {
    const folders = [{ id: '1', name: 'Java' }]
    expect(validateFolderName('java', folders).valid).toBe(false)
  })
})

describe('question validators', () => {
  it('requires a question', () => {
    expect(validateQuestion('').valid).toBe(false)
  })

  it('allows empty answers', () => {
    expect(normalizeAnswer('').valid).toBe(true)
  })
})

describe('archive validation', () => {
  it('accepts a valid archive', () => {
    const data = {
      version: 1,
      folders: [{ id: 'f1', name: 'Java', sort: 0, createdAt: 1, updatedAt: 1 }],
      qaItems: [{ id: 'q1', folderId: 'f1', question: '什么是 JVM？', answer: 'Java 虚拟机', createdAt: 1, updatedAt: 1 }],
    }
    expect(validateArchive(data)).toEqual({ valid: true })
  })

  it('rejects orphaned questions', () => {
    const data = {
      version: 1,
      folders: [],
      qaItems: [{ id: 'q1', folderId: 'missing', question: 'Q', answer: '', createdAt: 1, updatedAt: 1 }],
    }
    expect(validateArchive(data).valid).toBe(false)
  })
})
