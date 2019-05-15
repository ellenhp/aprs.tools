---
title: Updates
layout: post
permalink: /updates/
---

{% for post in site.posts %}
  <article>
    <h2>
      <a href="{{ post.url }}">
        {{ post.title }}
      </a>
    </h2>
    <p class="blogdate">{{ post.date | date: "%d %B %Y" }}</p>
    <p>{{ post.excerpt }}</p>
  </article>
{% endfor %}

