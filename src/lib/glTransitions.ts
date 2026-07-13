// ═══ GL TRANSITIONS — catalogue complet gl-transitions.com ════════════════════
// Paquet npm officiel : gl-transitions (125 transitions, MIT License)
// NE PAS modifier les shaders à la main — ils viennent tous du package npm.

// @ts-ignore — module CJS, pas de types déclarés
import _glLib from 'gl-transitions';
const _glRaw: Array<{
  name: string;
  glsl: string;
  defaultParams: Record<string, any>;
  paramsTypes: Record<string, string>;
}> = _glLib as any;

// ─── Interface ────────────────────────────────────────────────────────────────
export interface TransitionDef {
  id: string;
  label: string;
  category: string;
  icon: string;
  cssPreview: string;
  glsl: string;
  defaultParams: Record<string, any>;
  paramsTypes: Record<string, string>;
}

// ─── Vertex shader ─────────────────────────────────────────────────────────────
const VERTEX_SHADER = `
attribute vec2 a_position;
varying vec2 vTexCoord;
void main() {
  vTexCoord = a_position * 0.5 + 0.5;
  gl_Position = vec4(a_position, 0.0, 1.0);
}`;

// ─── Header fragment — getFromColor / getToColor requis par gl-transitions ────
const FRAG_HEADER = `
precision mediump float;
uniform sampler2D from;
uniform sampler2D to;
uniform float progress;
uniform vec2 resolution;
varying vec2 vTexCoord;

vec4 getFromColor(vec2 uv) { return texture2D(from, vec2(uv.x, 1.0 - uv.y)); }
vec4 getToColor(vec2 uv)   { return texture2D(to,   vec2(uv.x, 1.0 - uv.y)); }
`;

// ─── Catégorisation automatique par nom ───────────────────────────────────────
function categorize(name: string): string {
  const n = name.toLowerCase().replace(/[_\-\s]/g, '');
  if (/fade|blur|morph|dissolve|exposure|multiply|grayscale|hsv|melt|luminance|colordist|static/.test(n)) return 'Fondu';
  if (/wipe|slide|horizontal|vertical|leftright|topbottom|directional|squeeze|translation|warp|wind|splitslide|orizontal|ertical/.test(n)) return 'Glissement';
  if (/zoom|scale|vanish|bounce|crosswarp|crosszoom|revolve|dreamy|simple/.test(n)) return 'Zoom';
  if (/glitch|datamosh|tvstatic|filmburn|flicker|signallost|oldtv|burn|parametric/.test(n)) return 'Créatif';
  if (/cube|flip|book|curl|fold|door|swap|stereo|rolls|bowtie|doom|puzzle|gridflip|mosaic|tiles|box|advanced|block|windowblind/.test(n)) return '3D';
  if (/ripple|waterdrop|swirl|perlin|displacement|colorphase|colour|tangent|undulat|edge|coord|noise|dreamy|luma|overexpos|luminance|melt/.test(n)) return 'Effets';
  if (/circle|star|heart|polka|chess|crosshatch|pixel|square|blinds|slice|hexagon|butter|cannabis|polar|kaleido|angular|pinwheel|flyeye|rotate|fragment|rectangle|crop|radial|squarewire|random|windowslice/.test(n)) return 'Formes';
  return 'Effets';
}

function prettify(name: string): string {
  return name
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/[_\-]/g, ' ')
    .replace(/\b\w/g, c => c.toUpperCase())
    .trim();
}

const CAT_ICONS: Record<string, string> = {
  'Fondu': '🌫', 'Glissement': '↔', 'Zoom': '🔍',
  'Créatif': '⚡', '3D': '🎲', 'Effets': '🌊', 'Formes': '⭐',
};

function cssHint(name: string): string {
  const n = name.toLowerCase().replace(/[_\-\s]/g, '');
  if (/left|wipeleft|slideleft/.test(n)) return 'slideleft';
  if (/right|wiperight|slideright/.test(n)) return 'slideright';
  if (/up|wipeup|slideup/.test(n)) return 'slideup';
  if (/down|wipedown|slidedown/.test(n)) return 'slidedown';
  if (/zoom|scale|crosszoom|dreamy|crosswarp|vanish|bounce/.test(n)) return 'zoomin';
  if (/cube|fold|door|swap|rolls|bowtie|puzzle/.test(n)) return 'cube';
  if (/book|curl|page/.test(n)) return 'pageflip';
  if (/glitch|datamosh|parametric/.test(n)) return 'glitch';
  if (/swirl|spiral/.test(n)) return 'swirl';
  if (/ripple|water|wave/.test(n)) return 'ripple';
  if (/pixel|mosaic|block|chess|grid|tile/.test(n)) return 'pixelize';
  if (/circle|radial|crop|circleopen/.test(n)) return 'circleopen';
  if (/pinwheel|angular|rotat|revolve/.test(n)) return 'pinwheel';
  if (/kaleido/.test(n)) return 'kaleidoscope';
  if (/burn|undulat/.test(n)) return 'burn';
  if (/dot|polka/.test(n)) return 'polkadots';
  if (/heart|star|cannabis|butterfly|hexagon|flyeye|polar|fragment/.test(n)) return 'circleopen';
  if (/blur|morph|dissolve|melt|luminance|colour|static|film/.test(n)) return 'dissolve';
  return 'fade';
}

// ─── Catalogue complet (125 transitions) ──────────────────────────────────────
export const ALL_TRANSITIONS: TransitionDef[] = _glRaw.map(tr => ({
  id: tr.name,
  label: prettify(tr.name),
  category: categorize(tr.name),
  icon: CAT_ICONS[categorize(tr.name)] || '✨',
  cssPreview: cssHint(tr.name),
  glsl: tr.glsl,
  defaultParams: tr.defaultParams || {},
  paramsTypes: tr.paramsTypes || {},
}));

// Ordre fixe des catégories
export const TRANSITION_CATEGORIES: string[] = [
  'Fondu', 'Glissement', 'Zoom', '3D', 'Formes', 'Créatif', 'Effets'
].filter(cat => ALL_TRANSITIONS.some(t => t.category === cat));

export function getTransition(id: string): TransitionDef {
  return (
    ALL_TRANSITIONS.find(t => t.id === id) ||
    ALL_TRANSITIONS.find(t => t.id === 'fade') ||
    ALL_TRANSITIONS[0]
  );
}

// ─── Renderer WebGL ───────────────────────────────────────────────────────────
export class GLTransitionRenderer {
  private gl: WebGLRenderingContext | null = null;
  private canvas: HTMLCanvasElement | null = null;
  private programs: Map<string, WebGLProgram | null> = new Map();
  private fromTex: WebGLTexture | null = null;
  private toTex: WebGLTexture | null = null;
  private quadBuf: WebGLBuffer | null = null;

  init(canvas: HTMLCanvasElement): boolean {
    this.canvas = canvas;
    const gl = canvas.getContext('webgl', { premultipliedAlpha: false, alpha: true });
    if (!gl) return false;
    this.gl = gl;
    this.quadBuf = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, this.quadBuf);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]), gl.STATIC_DRAW);
    this.fromTex = this.createTex(gl);
    this.toTex = this.createTex(gl);
    return true;
  }

  private createTex(gl: WebGLRenderingContext): WebGLTexture {
    const t = gl.createTexture()!;
    gl.bindTexture(gl.TEXTURE_2D, t);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    // 1×1 pixel transparent pour éviter les erreurs avant la première frame
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, 1, 1, 0, gl.RGBA, gl.UNSIGNED_BYTE, new Uint8Array([0, 0, 0, 255]));
    return t;
  }

  private getProgram(transId: string): WebGLProgram | null {
    const gl = this.gl; if (!gl) return null;
    if (this.programs.has(transId)) return this.programs.get(transId) ?? null;
    const tr = getTransition(transId);
    const fsSrc = FRAG_HEADER + '\n' + tr.glsl + '\nvoid main() { gl_FragColor = transition(vTexCoord); }';
    const vs = this.compileShader(gl, gl.VERTEX_SHADER, VERTEX_SHADER);
    const fs = this.compileShader(gl, gl.FRAGMENT_SHADER, fsSrc);
    if (!vs || !fs) { this.programs.set(transId, null); return null; }
    const prog = gl.createProgram()!;
    gl.attachShader(prog, vs); gl.attachShader(prog, fs); gl.linkProgram(prog);
    if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) {
      console.warn('[GLTransition]', transId, gl.getProgramInfoLog(prog)?.slice(0, 200));
      this.programs.set(transId, null);
      return null;
    }
    this.programs.set(transId, prog);
    return prog;
  }

  private compileShader(gl: WebGLRenderingContext, type: number, src: string): WebGLShader | null {
    const s = gl.createShader(type)!;
    gl.shaderSource(s, src); gl.compileShader(s);
    if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) {
      console.warn('[GLShader]', gl.getShaderInfoLog(s)?.slice(0, 200));
      return null;
    }
    return s;
  }

  uploadFrame(which: 'from' | 'to', source: HTMLVideoElement | HTMLImageElement | HTMLCanvasElement): void {
    const gl = this.gl; if (!gl) return;
    const tex = which === 'from' ? this.fromTex : this.toTex;
    gl.bindTexture(gl.TEXTURE_2D, tex);
    // Ne pas flipper ici — on inverse dans le shader via (uv.x, 1.0 - uv.y)
    gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, false);
    try {
      gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, source as TexImageSource);
    } catch {}
  }

  render(transId: string, progress: number, overrideParams: Record<string, any> = {}): void {
    const gl = this.gl; if (!gl || !this.canvas) return;
    const prog = this.getProgram(transId); if (!prog) return;

    gl.viewport(0, 0, this.canvas.width, this.canvas.height);
    gl.useProgram(prog);

    // Textures
    gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, this.fromTex);
    gl.uniform1i(gl.getUniformLocation(prog, 'from'), 0);
    gl.activeTexture(gl.TEXTURE1); gl.bindTexture(gl.TEXTURE_2D, this.toTex);
    gl.uniform1i(gl.getUniformLocation(prog, 'to'), 1);

    // Uniforms standards
    gl.uniform1f(gl.getUniformLocation(prog, 'progress'), Math.max(0, Math.min(1, progress)));
    gl.uniform2f(gl.getUniformLocation(prog, 'resolution'), this.canvas.width, this.canvas.height);

    // Uniforms custom (float / vec2 / vec3 / vec4 / bool)
    const tr = getTransition(transId);
    const params = { ...(tr.defaultParams || {}), ...overrideParams };
    const types = tr.paramsTypes || {};
    for (const [k, v] of Object.entries(params)) {
      const loc = gl.getUniformLocation(prog, k);
      if (!loc) continue;
      const t = types[k];
      if (t === 'bool' || typeof v === 'boolean') {
        gl.uniform1i(loc, v ? 1 : 0);
      } else if (typeof v === 'number' || t === 'float') {
        gl.uniform1f(loc, Number(v));
      } else if (Array.isArray(v)) {
        if (v.length === 2) gl.uniform2fv(loc, v as number[]);
        else if (v.length === 3) gl.uniform3fv(loc, v as number[]);
        else if (v.length === 4) gl.uniform4fv(loc, v as number[]);
      }
    }

    // Draw quad
    gl.bindBuffer(gl.ARRAY_BUFFER, this.quadBuf);
    const pos = gl.getAttribLocation(prog, 'a_position');
    gl.enableVertexAttribArray(pos);
    gl.vertexAttribPointer(pos, 2, gl.FLOAT, false, 0, 0);
    gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
  }

  destroy(): void {
    const gl = this.gl; if (!gl) return;
    this.programs.forEach(p => { if (p) gl.deleteProgram(p); });
    this.programs.clear();
    if (this.fromTex) gl.deleteTexture(this.fromTex);
    if (this.toTex) gl.deleteTexture(this.toTex);
    if (this.quadBuf) gl.deleteBuffer(this.quadBuf);
    this.gl = null;
  }
}

// ── Système de miniatures (snapshots WebGL) pour le picker ────────────────────
const _thumbCache = new Map<string, string>();
const _thumbSubs  = new Map<string, Array<(url: string) => void>>();
let   _thumbQueue: string[]                   = [];
let   _thumbRunning                           = false;
let   _thumbGL: GLTransitionRenderer | null   = null;
let   _thumbCv: HTMLCanvasElement    | null   = null;

function _mkThumbGrad(c1: string, c2: string): HTMLCanvasElement {
  const cv = document.createElement('canvas');
  cv.width = 80; cv.height = 52;
  const ctx = cv.getContext('2d');
  if (!ctx) return cv;
  const g = ctx.createLinearGradient(0, 0, 80, 52);
  g.addColorStop(0, c1); g.addColorStop(1, c2);
  ctx.fillStyle = g; ctx.fillRect(0, 0, 80, 52);
  return cv;
}

function _thumbNext(): void {
  if (_thumbRunning || _thumbQueue.length === 0) return;
  _thumbRunning = true;
  const id = _thumbQueue.shift()!;
  if (_thumbCache.has(id)) {
    const url = _thumbCache.get(id)!;
    (_thumbSubs.get(id) || []).forEach(cb => cb(url));
    _thumbSubs.delete(id);
    _thumbRunning = false;
    _scheduleThumbNext();
    return;
  }
  if (!_thumbGL || !_thumbCv) {
    _thumbCv = document.createElement('canvas');
    _thumbCv.width = 80; _thumbCv.height = 52;
    _thumbGL = new GLTransitionRenderer();
    if (_thumbGL.init(_thumbCv)) {
      _thumbGL.uploadFrame('from', _mkThumbGrad('#1e3a6e', '#0a1b3b'));
      _thumbGL.uploadFrame('to',   _mkThumbGrad('#6e1e1e', '#3b0a0a'));
    } else {
      _thumbGL = null; _thumbCv = null;
    }
  }
  if (_thumbGL && _thumbCv) {
    try {
      _thumbGL.render(id, 0.5);
      const url = _thumbCv.toDataURL('image/jpeg', 0.85);
      _thumbCache.set(id, url);
      (_thumbSubs.get(id) || []).forEach(cb => cb(url));
      _thumbSubs.delete(id);
    } catch {}
  }
  _thumbRunning = false;
  _scheduleThumbNext();
}

function _scheduleThumbNext(): void {
  if (_thumbQueue.length === 0) return;
  if (typeof requestIdleCallback !== 'undefined') {
    requestIdleCallback(_thumbNext, { timeout: 120 });
  } else {
    setTimeout(_thumbNext, 8);
  }
}

export function getThumbUrlSync(id: string): string {
  return _thumbCache.get(id) || '';
}

export function requestTransitionThumb(id: string, cb: (url: string) => void): void {
  const cached = _thumbCache.get(id);
  if (cached) { cb(cached); return; }
  if (!_thumbSubs.has(id)) _thumbSubs.set(id, []);
  _thumbSubs.get(id)!.push(cb);
  if (!_thumbQueue.includes(id)) {
    _thumbQueue.push(id);
    _scheduleThumbNext();
  }
}
