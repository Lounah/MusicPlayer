# MusicPlayer
Супер-супер-супер простой музыкальный плеер. Воспроизводит mp3 файлы из определенной папки или всех, доступных пользователю

<li> Старался сделать его как можно более быстрым -- почти все приложение состоит из отрисованных на канвасе элементов </li>

# Скриншоты
<p>
<a href="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/main_screen.jpg" target="_blank">
  <img src="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/main_screen.jpg" width="270" height="480" alt="Screenshot" style="max-width:100%;">
</a>
<a href="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/folder_selector_screen.jpg" target="_blank">
  <img src="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/folder_selector_screen.jpg" width="270" height="480" alt="Screenshot" style="max-width:100%;">
</a>
<a href="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/audio_list_screen.jpg" target="_blank">
  <img src="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/audio_list_screen.jpg" width="270" height="480" alt="Screenshot" style="max-width:100%;">
</a>
<a href="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/audio_player_screen.jpg" target="_blank">
  <img src="https://github.com/Lounah/MusicPlayer/blob/master/screenshots/audio_player_screen.jpg" width="270" height="480" alt="Screenshot" style="max-width:100%;">
</a>
</p>

# Использованные библиотеки

<li> <a href="">Exoplayer (core & ui)</a></li>

# TBD 
Тесты
Также было бы неплохо дать пользователю возможность контролировать текущее время трека, то есть дописать seekBar с таймлайном
Также было бы неплохо добавить свайп по обложке альбома, чтобы переключить текущий трек

# Примечания
Многое из того, что было сделано мной в этом проекте, делалось мной впервые. Поэтому получилась довольно сумбурная архитектура, в которой, как мне кажется, все модули довольно тесно связаны между собой.
Также, писав приложение, я много эксперементировал -- бесшовные вью, старался также не использовать xml в некоторых местах, а накидывать все "руками". Получилось довольно странно

# Как оно работает
Для себя я разделил взаимодействие пользователя с плеером на три части:
UI -- здесь сидит на BottomAudioView, который отображает информацию о текущем треке
AudioPlayer -- некий контроллер, то, что содержит инстанс нашего движка, воспроизводящего аудио, стоит лишь подать в него очередь из треков
PlayerService -- то, с чем взаимодействует наш UI посредством IPC (не знаю, насколько это правильное решение, я вроде как забыл указать для сервиса другой процесс, но для его коммуникации с активити я использовал мессенджер)

Из UI поступают команды на обновление текущего трека, они переходят в сервис, сервис передает эту команду плееру, плеер отобразит это в нотификашке и передаст сообщение обратно в UI 

Есть рассинхрон в таймлайне трека и тем, что отображается в UI
Есть места с потенциальными утечками
Например, вроде как сервис не анбиндится в нужное для этого время
Все пронизано синглтонами, и я не знаю, насколько это хорошо

# App apk link
https://yadi.sk/d/ewTEePrBJ_GRrQ
