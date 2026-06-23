import { useMemo } from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { ExternalLink, Clipboard, Check, Share2 } from 'lucide-react'
import { useState } from 'react'
import { cn } from '@/lib/utils'

interface AnswerViewProps {
  content: string | undefined
}

export const AnswerView = ({ content }: AnswerViewProps) => {
  const [copied, setCopied] = useState(false)
  const safeContent = typeof content === 'string' ? content : ''
  
  const { cleanContent, sources } = useMemo(() => {
    if (!safeContent) return { cleanContent: '', sources: [] }
    
    const lines = safeContent.split('\n')
    const sourcesIdx = lines.findIndex(l => /sources?|references?/i.test(l))
    if (sourcesIdx === -1) return { cleanContent: safeContent, sources: [] }
    
    const clean = lines.slice(0, sourcesIdx).join('\n')
    const extractedLines = lines.slice(sourcesIdx + 1)
    const extracted = extractedLines
      .map((line, idx) => {
        // [1] Title: URL or [Title](URL)
        const match = line.match(/\[([^\]]+)\]\((https?:\/\/[^\)]+)\)/) || 
                      line.match(/(?:\[(\d+)\]\s*)?(?:(.+?):\s*)?(https?:\/\/[^\s]+)/)
        
        if (match) {
          return { 
            title: match[1] || match[2] || new URL(match[3]).hostname, 
            url: match[2] || match[3], 
            index: idx + 1 
          }
        }
        return null
      }).filter(Boolean)
    
    return { cleanContent: clean, sources: extracted }
  }, [safeContent])

  const copyToClipboard = () => {
    navigator.clipboard.writeText(safeContent)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  if (!cleanContent) return null

  return (
    <div className="relative group/answer animate-in fade-in slide-in-from-bottom-2 duration-700">
      <div className="prose prose-sm dark:prose-invert max-w-none prose-p:leading-relaxed prose-pre:bg-muted/40 prose-pre:p-4 prose-pre:rounded-xl prose-code:text-primary prose-a:text-primary prose-a:no-underline hover:prose-a:underline">
        <ReactMarkdown remarkPlugins={[remarkGfm]}>
          {cleanContent}
        </ReactMarkdown>
      </div>

      {sources.length > 0 && (
        <div className="mt-8 pt-6 border-t border-border/40">
          <div className="flex items-center gap-2 mb-4 text-sm font-semibold text-foreground/80">
            <ExternalLink size={16} className="text-primary" />
            <span>Sources et Références</span>
          </div>
          <div className="flex flex-wrap gap-2">
            {sources.map((source, i) => (
              <a 
                key={i} 
                href={source.url} 
                target="_blank" 
                rel="noopener noreferrer"
                className="flex items-center gap-2 px-3 py-2 rounded-xl bg-muted/40 border border-border/20 hover:border-primary/40 hover:bg-muted/60 transition-all text-xs text-muted-foreground hover:text-foreground"
              >
                <span className="h-5 w-5 rounded-md bg-background flex items-center justify-center text-[10px] font-bold text-primary border border-primary/20 shrink-0">
                  {i + 1}
                </span>
                <span className="truncate max-w-[240px] font-medium">{source.title}</span>
              </a>
            ))}
          </div>
        </div>
      )}

      {/* Action Buttons */}
      <div className="mt-6 flex items-center gap-2 opacity-0 group-hover/answer:opacity-100 transition-opacity duration-300">
        <button 
          onClick={copyToClipboard}
          className="flex items-center gap-2 px-3 py-1.5 rounded-full hover:bg-muted text-muted-foreground hover:text-foreground transition-all text-xs"
        >
          {copied ? <Check size={14} className="text-green-500" /> : <Clipboard size={14} />}
          <span>{copied ? 'Copié' : 'Copier'}</span>
        </button>
        <button className="flex items-center gap-2 px-3 py-1.5 rounded-full hover:bg-muted text-muted-foreground hover:text-foreground transition-all text-xs">
          <Share2 size={14} />
          <span>Partager</span>
        </button>
      </div>
    </div>
  )
}
