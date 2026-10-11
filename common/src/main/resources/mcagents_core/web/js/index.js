/*
 * The front page: one entry per mod that registered pages, linking to its first page.
 */
(function () {
  'use strict';

  var C = window.MCAgents;

  async function render() {
    var container = document.getElementById('modules');
    var modules = await C.navigation();
    if (!modules.length) {
      C.showState(container, 'No installed mod has registered any pages yet.');
      return;
    }
    C.replace(container, modules.map(function (entry) {
      var item = C.el('p');
      var anchor = C.el('a', 'nav__link', entry.title);
      anchor.href = C.withToken(entry.home);
      item.appendChild(anchor);
      return item;
    }));
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', render);
  } else {
    render();
  }
})();
