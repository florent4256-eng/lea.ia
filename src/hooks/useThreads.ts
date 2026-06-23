import { useState, useEffect, useCallback } from 'react'

export interface Thread {
  id: string
  userId: string
  title: string
  createdAt: string
  updatedAt: string
}

export interface Message {
  id: string
  threadId: string
  role: 'user' | 'assistant' | 'system'
  content: string
  parts?: any[]
  createdAt: string
}

export function useThreads() {
  const user = { id: localStorage.getItem('lea_currentUser') || '' };
  const [threads, setThreads] = useState<Thread[]>([])
  const [isLoading, setIsLoading] = useState(true)

  // Gestion locale des messages via localStorage pour l'instant
  const fetchThreads = useCallback(async () => {
    setIsLoading(true)
    try {
      const savedThreads = localStorage.getItem('lea_threads')
      if (savedThreads) {
        setThreads(JSON.parse(savedThreads))
      }
    } catch (err) {
      console.error('Erreur de lecture locale:', err)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchThreads()
  }, [fetchThreads])

  const createThread = async (title: string) => {
    const newThread: Thread = {
      id: Date.now().toString(),
      userId: user.id,
      title: title.slice(0, 50) || 'Nouvelle discussion Léa',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
    
    const updatedThreads = [newThread, ...threads]
    setThreads(updatedThreads)
    localStorage.setItem('lea_threads', JSON.stringify(updatedThreads))
    return newThread
  }

  const deleteThread = async (id: string) => {
    const updatedThreads = threads.filter(t => t.id !== id)
    setThreads(updatedThreads)
    localStorage.setItem('lea_threads', JSON.stringify(updatedThreads))
    // Nettoyer aussi les messages liés
    localStorage.removeItem(`messages_${id}`)
  }

  const saveMessage = async (threadId: string, message: any) => {
    try {
      const threadMessages = await getThreadMessages(threadId)
      const newMessage = {
        ...message,
        id: Date.now().toString(),
        threadId,
        createdAt: new Date().toISOString()
      }
      const updatedMessages = [...threadMessages, newMessage]
      localStorage.setItem(`messages_${threadId}`, JSON.stringify(updatedMessages))
    } catch (err) {
      console.error('Erreur sauvegarde message:', err)
    }
  }

  const getThreadMessages = async (threadId: string): Promise<Message[]> => {
    try {
      const savedMessages = localStorage.getItem(`messages_${threadId}`)
      return savedMessages ? JSON.parse(savedMessages) : []
    } catch (err) {
      console.error('Erreur lecture messages:', err)
      return []
    }
  }

  return {
    threads,
    isLoading,
    createThread,
    deleteThread,
    saveMessage,
    getThreadMessages,
    refreshThreads: fetchThreads
  }
}