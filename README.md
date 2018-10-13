# MusicPlayer
Супер-супер-супер простой музыкальный плеер. Воспроизводит mp3 файлы из определенной папки или всех, доступных пользователю

<li> Старался сделать его как можно более быстрым -- почти все приложение состоит из отрисованных на канвасе элементов </li>
<li> Переживает смену конфигурации экрана </li>
<li> Реагирует на аудиофокус </li>
<li> Свайп для открытия-закрытия вью с текущим треком </li>
<li> Кеширует обложки, загружает из бекграунд потока </li>

# Скриншоты
<p>
<a href="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/app_player.jpg" target="_blank">
  <img src="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/app_player.jpg" width="270" height="480" alt="Screenshot" style="max-width:100%;">
</a>
<a href="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/app_initial.jpg" target="_blank">
  <img src="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/app_initial.jpg" width="270" height="480" alt="Screenshot" style="max-width:100%;">
</a>
<a href="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/app_tracklist.jpg" target="_blank">
  <img src="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/app_tracklist.jpg" width="270" height="480" alt="Screenshot" style="max-width:100%;">
</a>
<a href="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/app_tracklist_landscape.jpg" target="_blank">
  <img src="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/app_tracklist_landscape.jpg" width="480" height="270" alt="Screenshot" style="max-width:100%;">
</a>
<a href="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/app_filebrowser.jpg" target="_blank">
  <img src="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/app_filebrowser.jpg" width="270" height="480" alt="Screenshot" style="max-width:100%;">
</a>
<a href="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/app_empty_state.jpg" target="_blank">
  <img src="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/app_empty_state.jpg" width="270" height="480" alt="Screenshot" style="max-width:100%;">
</a>
</p>

# Использованные библиотеки

<li> <a href="">Exoplayer (core & ui)</a></li>

# TBD 
Тесты
Добавить поддержку next/previous в нотификации
Добавить "карусель"-свайп по обложкам

# Примечания
По тз не сделано:

<li> Свайп по обложкам, а также анимация переключения треков </li>
<li> Блюр позади обложки, а также тень-блюр под кнопками </li>
<li> Наверное, что-то еще </li>

# Как оно работает
Для себя я разделил взаимодействие пользователя с плеером на три части:
UI -- здесь сидит на BottomAudioView, который отображает информацию о текущем треке
AudioPlayer -- некий контроллер, то, что содержит инстанс нашего движка, воспроизводящего аудио, стоит лишь подать в него очередь из треков
PlayerService -- то, с чем взаимодействует наш UI посредством IPC (не знаю, насколько это правильное решение, я вроде как забыл указать для сервиса другой процесс, но для его коммуникации с активити я использовал мессенджер)

Из UI поступают команды на обновление текущего трека, они переходят в сервис, сервис передает эту команду плееру, плеер отобразит это в нотификашке и передаст сообщение обратно в UI 

# App apk link
https://yadi.sk/d/ewTEePrBJ_GRrQ
