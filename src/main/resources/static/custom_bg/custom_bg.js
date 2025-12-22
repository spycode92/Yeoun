/* home 화면 ui 테마변경 js */

'use strict';

const THEMES = ['theme-dark', 'theme-green'];

document.addEventListener('DOMContentLoaded', () => {
  const savedTheme = localStorage.getItem('theme');
  if (savedTheme) {
    setTheme(savedTheme);
  }
});

function setTheme(theme) {
  const html = document.documentElement;

  // 기존 테마 제거
  THEMES.forEach(t => html.classList.remove(t));

  // theme가 있으면 적용 (없으면 :root 상태)
  if (theme) {
    html.classList.add(theme);
  }

  localStorage.setItem('theme', theme || '');
}

const lightBtn = document.getElementById('light-btn');
const darkBtn  = document.getElementById('dark-btn');
const greenBtn = document.getElementById('green-btn');

if (lightBtn && darkBtn && greenBtn) {

  // 초기 테마 (:root)
  lightBtn.addEventListener('click', () => {
    setTheme('');
  });

  darkBtn.addEventListener('click', () => {
    setTheme('theme-dark');
  });

  greenBtn.addEventListener('click', () => {
    setTheme('theme-green');
  });
}















