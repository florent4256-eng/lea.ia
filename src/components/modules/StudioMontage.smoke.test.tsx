import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StudioMontage } from './StudioMontage';

// Test de fumée : vérifie que le plus gros module du projet (éditeur vidéo,
// 3200+ lignes) monte sans planter et affiche sa structure de base.
// N'exerce pas la logique métier (montage, export, transitions) — juste le rendu initial.

describe('StudioMontage', () => {
  it('se monte sans lever d\'exception', () => {
    expect(() => render(<StudioMontage />)).not.toThrow();
  });

  it('appelle onClose quand fourni, sans planter au montage', () => {
    const onClose = vi.fn();
    expect(() => render(<StudioMontage onClose={onClose} />)).not.toThrow();
    expect(onClose).not.toHaveBeenCalled();
  });

  it('affiche la timeline (piste vidéo) au premier rendu', () => {
    render(<StudioMontage />);
    // La piste vidéo par défaut existe toujours dans un projet neuf.
    expect(screen.queryAllByText(/vidéo/i).length).toBeGreaterThan(0);
  });
});
