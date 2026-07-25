# 發布測試全綠的 lesson checkpoints

RushBook 的 main branch 是一門線性課程：每一課都是一個可執行、有完整文件、
測試全綠的 commit，並具有 annotated `lesson-NN-slug` tag。Lesson 文件包含
核心觀念、architecture delta、commands、evidence、理解題與完整答案。我們
使用 tags，而不維護永久 lesson branches，讓學習者能比較 diffs 或建立自己的
exercise branches，又不會分散專案的 maintained history。
