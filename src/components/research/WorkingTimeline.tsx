import { Search, Check, Loader2, ChevronDown, ChevronUp, Globe } from 'lucide-react'
import { useState } from 'react'
import { cn } from '@/lib/utils'

interface WorkingTimelineProps {
  parts: any[]
  isComplete: boolean
  mode: 'search' | 'research'
}

export const WorkingTimeline = ({ parts, isComplete, mode }: WorkingTimelineProps) => {
  const [isOpen, setIsOpen] = useState(true)

  // Extract search queries and sources from message.parts
  const toolCalls = parts.filter(p => p.type === 'tool-invocation')
  const searchQueries = toolCalls
    .filter(t => t.toolName === 'webSearch')
    .map(t => t.args?.query)
    .filter(Boolean)
  
  const sources = toolCalls
    .filter(t => t.toolName === 'webSearch' && t.state === 'result')
    .flatMap(t => t.result?.results || [])
    .filter((v, i, a) => a.findIndex(t => t.url === v.url) === i) // Unique sources
    .slice(0, 8)

  if (searchQueries.length === 0 && !isComplete) {
    return (
      <div className="flex items-center gap-3 py-4 text-sm text-muted-foreground animate-pulse">
        <Loader2 size={16} className="animate-spin text-primary" />
        <span>Léa réfléchit...</span>
      </div>
    )
  }

  if (searchQueries.length === 0 && isComplete) return null

  return (
    <div className="my-6 border border-border/40 rounded-2xl overflow-hidden bg-muted/20 dark:bg-muted/5 transition-all duration-500">
      <button 
        onClick={() => setIsOpen(!isOpen)}
        className="w-full flex items-center justify-between px-4 py-3 hover:bg-black/5 dark:hover:bg-white/5 transition-colors"
      >
        <div className="flex items-center gap-3">
          {isComplete ? (
            <div className="h-5 w-5 rounded-full bg-green-500/10 flex items-center justify-center">
              <Check size={12} className="text-green-600 dark:text-green-400" />
            </div>
          ) : (
            <Loader2 size={14} className="animate-spin text-primary" />
          )}
          <span className="text-sm font-medium text-foreground">
            {isComplete ? "Recherche terminée" : "Recherche en cours..."}
          </span>
          <span className="text-xs text-muted-foreground ml-2">
            {sources.length} sources trouvées
          </span>
        </div>
        {isOpen ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
      </button>

      {isOpen && (
        <div className="px-4 pb-4 animate-in slide-in-from-top-2 duration-300">
          {/* Search Pills */}
          <div className="flex flex-wrap gap-2 mb-4">
            {searchQueries.map((q, i) => (
              <div 
                key={i} 
                className="flex items-center gap-2 px-3 py-1.5 bg-background dark:bg-[#2d2f31] rounded-full text-xs border border-border/50 shadow-sm"
              >
                <Search size={12} className="text-primary" />
                <span className="truncate max-w-[200px] text-foreground/80">{q}</span>
              </div>
            ))}
          </div>

          {/* Source Cards */}
          {sources.length > 0 && (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
              {sources.map((s, i) => (
                <a 
                  key={i} 
                  href={s.url} 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="flex flex-col gap-2 p-3 rounded-xl bg-background dark:bg-[#2d2f31] border border-border/50 hover:border-primary/50 hover:shadow-md transition-all group overflow-hidden"
                >
                  <div className="flex items-center gap-2">
                    <div className="h-6 w-6 rounded-md bg-muted flex items-center justify-center p-1 shrink-0 group-hover:bg-primary/10 transition-colors">
                      <img 
                        src={`https://www.google.com/s2/favicons?domain=${new URL(s.url).hostname}&sz=64`} 
                        className="w-4 h-4" 
                        alt=""
                        onError={(e) => {
                          e.currentTarget.style.display = 'none'
                          e.currentTarget.parentElement?.classList.add('bg-primary/20')
                        }}
                      />
                    </div>
                    <span className="text-[10px] text-muted-foreground uppercase tracking-tight truncate">
                      {new URL(s.url).hostname.replace('www.', '')}
                    </span>
                  </div>
                  <span className="text-xs font-medium text-foreground leading-tight line-clamp-2">
                    {s.title}
                  </span>
                </a>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
