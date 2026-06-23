import { useState, useRef, useEffect } from 'react'
import { Search, ArrowRight, Telescope, Globe, Paperclip, Send, Loader2, Sparkles, Wand2 } from 'lucide-react'
import { cn } from '@/lib/utils'

const BRAND_COLOR = '#407E8B' // From archetype
const LEA_GRADIENT = "linear-gradient(135deg, #0047ff 0%, #9400d3 50%, #000000 100%)";

interface SearchBarProps {
  onSearch: (query: string, mode: 'search' | 'research') => void
  isLoading: boolean
  compact?: boolean
  initialQuery?: string
}

export const SearchBar = ({ onSearch, isLoading, compact, initialQuery }: SearchBarProps) => {
  const [query, setQuery] = useState(initialQuery || '')
  const [mode, setMode] = useState<'search' | 'research'>('search')
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const [isFocused, setIsFocused] = useState(false)

  // Tooltip state for Quick/Pro popover
  const [activeTooltip, setActiveTooltip] = useState<'search' | 'research' | null>(null)
  const tooltipTimeoutRef = useRef<any>(null)

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto'
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 200)}px`
    }
  }, [query])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!query.trim() || isLoading) return
    onSearch(query.trim(), mode)
    setQuery('')
    if (textareaRef.current) textareaRef.current.style.height = 'auto'
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSubmit(e)
    }
  }

  const handleMouseEnter = (m: 'search' | 'research') => {
    if (tooltipTimeoutRef.current) clearTimeout(tooltipTimeoutRef.current)
    setActiveTooltip(m)
  }

  const handleMouseLeave = () => {
    tooltipTimeoutRef.current = setTimeout(() => setActiveTooltip(null), 300)
  }

  return (
    <div className={cn(
      "relative w-full transition-all duration-300",
      compact ? "max-w-3xl" : "max-w-2xl"
    )}>
      <form 
        onSubmit={handleSubmit}
        className={cn(
          "relative flex flex-col bg-[#f0f4f9] dark:bg-[#1e1f20] transition-all duration-300",
          isFocused ? "shadow-md ring-1 ring-primary/20 bg-white dark:bg-[#282a2d]" : "shadow-sm",
          compact ? "rounded-[32px] p-3 pl-6 pr-3" : "rounded-[28px] p-4 pl-6 pr-4"
        )}
      >
        <div className="flex items-start gap-4">
          <textarea
            ref={textareaRef}
            rows={1}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
            placeholder="Saisissez une invite ici"
            className="flex-1 bg-transparent border-none outline-none focus:outline-none focus:ring-0 resize-none py-1 text-base placeholder:text-muted-foreground/60 min-h-[24px]"
          />
        </div>

        <div className="flex items-center justify-between mt-3 px-1 relative h-10">
          <div className="flex items-center gap-1 p-1 bg-muted/50 dark:bg-muted/10 rounded-[12px]">
            <button 
              type="button" 
              onClick={() => setMode('search')}
              onMouseEnter={() => handleMouseEnter('search')}
              onMouseLeave={handleMouseLeave}
              className={cn(
                "p-2 rounded-[10px] transition-all relative group",
                mode === 'search' ? "bg-background shadow-sm text-primary" : "text-muted-foreground/70 hover:text-foreground hover:bg-black/5 dark:hover:bg-white/5"
              )}
            >
              <Search size={18} strokeWidth={mode === 'search' ? 2.5 : 2} />
              {activeTooltip === 'search' && (
                <div className="absolute bottom-full mb-3 left-0 z-50 animate-in fade-in zoom-in duration-200">
                  <div className="bg-[#1e1f20] text-white p-3 rounded-xl shadow-xl border border-white/10 w-64">
                    <div className="font-medium text-sm">Mode Rapide</div>
                    <div className="text-xs text-zinc-400 mt-1">Réponses directes et précises pour les questions de tous les jours.</div>
                    <div className="absolute -bottom-1 left-4 w-2 h-2 bg-[#1e1f20] rotate-45 border-r border-b border-white/10" />
                  </div>
                </div>
              )}
            </button>
            <button 
              type="button" 
              onClick={() => setMode('research')}
              onMouseEnter={() => handleMouseEnter('research')}
              onMouseLeave={handleMouseLeave}
              className={cn(
                "p-2 rounded-[10px] transition-all relative group",
                mode === 'research' ? "bg-background shadow-sm text-[#9b72cb]" : "text-muted-foreground/70 hover:text-foreground hover:bg-black/5 dark:hover:bg-white/5"
              )}
            >
              <Telescope size={18} strokeWidth={mode === 'research' ? 2.5 : 2} />
              {activeTooltip === 'research' && (
                <div className="absolute bottom-full mb-3 left-0 z-50 animate-in fade-in zoom-in duration-200">
                  <div className="bg-[#1e1f20] text-white p-3 rounded-xl shadow-xl border border-white/10 w-64">
                    <div className="font-medium text-sm flex items-center gap-2">
                      <Sparkles size={14} className="text-[#9b72cb]" />
                      Recherche Approfondie
                    </div>
                    <div className="text-xs text-zinc-400 mt-1">Rapports complets avec plusieurs sources et raisonnement avancé.</div>
                    <div className="absolute -bottom-1 left-4 w-2 h-2 bg-[#1e1f20] rotate-45 border-r border-b border-white/10" />
                  </div>
                </div>
              )}
            </button>
          </div>

          <div className="flex items-center gap-3">
            {!compact && (
              <button type="button" className="p-2 text-muted-foreground/70 hover:text-foreground rounded-full transition-colors">
                <Paperclip size={20} />
              </button>
            )}
            <button 
              type="submit" 
              disabled={!query.trim() || isLoading}
              className={cn(
                "p-2.5 rounded-full transition-all flex items-center justify-center",
                query.trim() 
                  ? "bg-primary text-primary-foreground shadow-md scale-105" 
                  : "bg-muted text-muted-foreground/40"
              )}
            >
              {isLoading ? (
                <Loader2 size={20} className="animate-spin" />
              ) : (
                <Send size={18} strokeWidth={2.5} />
              )}
            </button>
          </div>
        </div>
      </form>
      
      {!compact && (
        <div className="mt-4 flex flex-wrap justify-center gap-2 px-4 animate-in fade-in slide-in-from-top-4 duration-1000">
          <SuggestionPill label="Planifier un voyage" icon={<Globe size={14} />} onClick={() => setQuery("Aide-moi à planifier un voyage de 3 jours à Paris.")} />
          <SuggestionPill label="Idées de recettes" icon={<Wand2 size={14} />} onClick={() => setQuery("Donne-moi des idées de recettes saines avec du poulet.")} />
          <SuggestionPill label="Expliquer un concept" icon={<Sparkles size={14} />} onClick={() => setQuery("Explique-moi l'informatique quantique comme si j'avais 10 ans.")} />
        </div>
      )}
    </div>
  )
}

function SuggestionPill({ label, icon, onClick }: { label: string, icon: React.ReactNode, onClick: () => void }) {
  return (
    <button 
      onClick={onClick}
      className="flex items-center gap-2 px-4 py-2 rounded-full border border-border/50 hover:bg-black/5 dark:hover:bg-white/5 text-sm text-foreground/80 transition-all hover:border-border"
    >
      {icon}
      <span>{label}</span>
    </button>
  )
}
