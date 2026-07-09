# external-union-bank-ng

> Reimagining what a modern digital banking experience could look like after a frustrating real-world customer support experience with Union Bank Nigeria.

> **Disclaimer:** This is an independent, unofficial software project. It is not affiliated with, endorsed by, or sponsored by Union Bank Nigeria. The experience described below reflects my personal experience, and this repository is intended as an engineering and UX demonstration rather than a representation of the bank's internal systems.

---

# Why this repository exists

I didn't wake up one day wanting to redesign a bank.

This project exists because I experienced what I believe was an unnecessarily difficult customer journey during a situation where time actually mattered.

A family member was in the hospital, and I needed to transfer money from my mother's Union Bank Nigeria account, which she had explicitly given me permission to access for that purpose.

Instead of being able to complete a simple transfer limit increase digitally, what followed was a process that stretched across multiple days, multiple support channels, and multiple verification steps without resolving the issue efficiently.

As a cloud engineer with some software development experience, I couldn't stop thinking:

> **This doesn't have to be this hard.**

So instead of only complaining about the experience, I decided to build the experience I wish existed.

---

# What happened

The account had reached its transfer limit.

Naturally, I opened the mobile app expecting to increase the limit.

Instead, I discovered that the process required details from a debit/credit card that wasn't linked to the account. There was no alternative digital verification flow available.

So I contacted customer support.

## Phone support

I called customer care.

I waited for over 11 minutes without reaching an agent.

## WhatsApp support

I then contacted Union Bank through WhatsApp.

I was placed somewhere around position **300** in the queue.

Over the next several hours, the queue position changed inconsistently and responses remained extremely slow.

After more than 14 hours, I still didn't have a resolution.

## Social media

I reached out through Twitter/X both privately and publicly.

Eventually an account manager contacted me.

At that point I thought the issue would finally be resolved.

Instead, I was informed that because I wasn't the account owner, they couldn't proceed—even though my mother had explicitly authorized me to help her and had provided all necessary information.

## The work schedule problem

My mother works until around 6:00 PM.

Most bank branches close before then.

Taking time off work simply to complete an account maintenance request shouldn't be the only practical option in a digital banking era.

Eventually she had to take a day off work.

## Video verification

She completed a video verification call.

Even after successfully completing identity verification, the process still wasn't finished.

She was then told another department would contact her and send a PDF form.

That form requested information the bank already possessed and that was already visible inside the banking application.

The completed form was returned.

At the time this project was started, the request was still pending.

---

# Why this frustrated me

None of the individual steps seemed unreasonable in isolation.

Together, however, they created a customer journey that felt fragmented, repetitive, and unnecessarily manual.

Identity was verified.

Then more verification was required.

Information already held by the bank had to be submitted again.

Support existed across multiple channels, yet none of them appeared capable of resolving the request from start to finish.

The technology exists to make this dramatically simpler.

I've experienced banking systems in Spain where many sensitive account changes can be completed securely within minutes using combinations of:

* Biometric authentication
* One-time passwords (OTP)
* In-app approvals
* Digital signatures
* Push notifications
* Strong customer authentication

That contrast inspired this project.

---

# The goal

This repository is **not** about reverse engineering Union Bank Nigeria's systems.

It is **not** about exposing proprietary information.

It is **not** about attacking individuals.

Its purpose is to demonstrate how software, good UX, and thoughtful system design can dramatically improve the customer experience.

I want to answer one question:

> **If I were building this banking experience today, how would I design it?**

---

# What this project will demonstrate

Some of the ideas explored here include:

* In-app transfer limit management
* Secure biometric approvals
* OTP-based verification
* Digital authorization flows
* Intelligent identity verification
* Case tracking with real-time status updates
* Unified customer support
* Elimination of redundant paperwork
* Clear audit trails
* Modern API-first architecture

---

# Current Experience vs Proposed Experience

| Current Experience        | Proposed Experience                    |
| ------------------------- | -------------------------------------- |
| Reach transfer limit      | Tap "Increase Limit"                   |
| Call customer support     | Authenticate with biometrics           |
| Wait in long queues       | Instant identity verification          |
| Multiple support channels | Single guided workflow                 |
| Video verification        | In-app verification where appropriate  |
| PDF forms                 | Pre-filled digital forms               |
| Manual processing         | Automated approval where policy allows |
| No visibility             | Real-time progress tracking            |

---

# Who this project is for

* Software engineers
* Product designers
* UX researchers
* Digital banking teams
* Anyone interested in improving customer experiences through technology

---

# Contributions

Constructive feedback and contributions are welcome.

If you've experienced similar friction with digital banking regardless of the institution I would love to hear your ideas for building better systems.

---

# Final Thoughts

This repository was born out of frustration.

But frustration alone doesn't improve software.

Building better software does.

If this project sparks conversations about modernizing digital banking, simplifying customer journeys, or reducing unnecessary friction, then it has achieved its purpose.
