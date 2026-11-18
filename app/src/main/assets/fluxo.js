(function () {
  if (window.__fxLoaded) return;
  window.__fxLoaded = true;

  var cfg = { css: '', blockShorts: true };
  var pending = false;

  // Elementos "cartão" que são escondidos quando contêm um link para /shorts/
  var CARD = [
    'ytm-rich-item-renderer',
    'ytm-video-with-context-renderer',
    'ytm-compact-video-renderer',
    'ytm-media-item',
    'ytm-reel-item-renderer',
    'ytm-shorts-lockup-view-model',
    'ytm-shorts-lockup-view-model-v2',
    'ytm-reel-shelf-renderer'
  ].join(',');

  // Regra 1: seletores simples (nunca falha por falta de suporte a :has)
  var SHORTS_CSS_SIMPLE = [
    'ytm-reel-shelf-renderer',
    'ytm-shorts-lockup-view-model',
    'ytm-shorts-lockup-view-model-v2',
    'ytm-reel-item-renderer'
  ].join(',\n') + ' { display: none !important; }';

  // Regra 2: seletores com :has (WebView do Chromium 105+)
  var SHORTS_CSS_HAS = [
    'ytm-rich-section-renderer:has(ytm-reel-shelf-renderer)',
    'ytm-rich-item-renderer:has(a[href*="/shorts/"])',
    'ytm-video-with-context-renderer:has(a[href*="/shorts/"])',
    'ytm-compact-video-renderer:has(a[href*="/shorts/"])',
    'ytm-media-item:has(a[href*="/shorts/"])',
    'ytm-pivot-bar-item-renderer:has(a[href="/shorts"])'
  ].join(',\n') + ' { display: none !important; }';

  function root() {
    return document.head || document.documentElement;
  }

  function setStyle(id, text) {
    var r = root();
    if (!r) return false;
    var s = document.getElementById(id);
    if (!s) {
      s = document.createElement('style');
      s.id = id;
      r.appendChild(s);
    }
    if (s.textContent !== text) s.textContent = text;
    return true;
  }

  function hide(el) {
    if (el.getAttribute('data-fx-hidden')) return;
    el.setAttribute('data-fx-hidden', '1');
    el.style.setProperty('display', 'none', 'important');
  }

  function restore() {
    var list = document.querySelectorAll('[data-fx-hidden]');
    for (var i = 0; i < list.length; i++) {
      list[i].style.removeProperty('display');
      list[i].removeAttribute('data-fx-hidden');
    }
  }

  function sweep() {
    var links = document.querySelectorAll('a[href*="/shorts/"]');
    for (var i = 0; i < links.length; i++) {
      var card = links[i].closest(CARD);
      if (card) hide(card);
    }
    var tabs = document.querySelectorAll('ytm-pivot-bar-item-renderer');
    for (var j = 0; j < tabs.length; j++) {
      var label = (tabs[j].textContent || '').trim().toLowerCase();
      if (label === 'shorts') hide(tabs[j]);
    }
  }

  function apply() {
    if (!setStyle('fx-theme', cfg.css)) {
      setTimeout(apply, 10);
      return;
    }
    if (cfg.blockShorts) {
      setStyle('fx-shorts-a', SHORTS_CSS_SIMPLE);
      setStyle('fx-shorts-b', SHORTS_CSS_HAS);
      sweep();
    } else {
      setStyle('fx-shorts-a', '');
      setStyle('fx-shorts-b', '');
      restore();
    }
  }

  function schedule() {
    if (pending) return;
    pending = true;
    setTimeout(function () {
      pending = false;
      if (cfg.blockShorts) sweep();
    }, 150);
  }

  // Clique em qualquer link de Short abre como vídeo normal
  document.addEventListener('click', function (e) {
    if (!cfg.blockShorts) return;
    var a = e.target && e.target.closest ? e.target.closest('a[href*="/shorts/"]') : null;
    if (!a) return;
    var m = (a.getAttribute('href') || '').match(/\/shorts\/([\w-]+)/);
    if (!m) return;
    e.preventDefault();
    e.stopPropagation();
    location.assign('/watch?v=' + m[1]);
  }, true);

  try {
    new MutationObserver(schedule).observe(document, { childList: true, subtree: true });
  } catch (err) {}

  window.__fxSet = function (c) {
    cfg = c;
    apply();
  };
})();
