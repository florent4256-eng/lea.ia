import React from 'react'

interface ShellProps {
  /** Contenu de la Sidebar */
  sidebar: React.ReactNode
  /** Nom de l'application */
  appName?: string
  /** Contenu principal */
  children: React.ReactNode
}

export function Shell({ sidebar, children }: ShellProps) {
  return (
    <div className="flex h-screen w-full bg-[#0a0a0c] overflow-hidden text-white">
      {/* Sidebar Latérale Fixe - Look Cockpit */}
      <aside className="h-full border-r border-white/10 bg-[#0a0a0c] min-w-[260px] hidden md:block">
        {sidebar}
      </aside>

      {/* Contenu Principal */}
      <main className="flex-1 h-full overflow-hidden relative bg-[#0a0a0c]">
        {/* Header Mobile simplifié (si besoin plus tard) */}
        <div className="md:hidden flex items-center h-14 px-4 border-b border-white/10 bg-[#0a0a0c]">
          <span className="font-bold tracking-widest text-xs uppercase">Léa v3</span>
        </div>

        {/* Page content */}
        <div className="h-full w-full">
          {children}
        </div>
      </main>
    </div>
  )
}