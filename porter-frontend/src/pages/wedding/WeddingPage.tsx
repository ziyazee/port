import { useState, useEffect, useRef } from "react";
import "./wedding.css";

const weddingDate = new Date("2026-08-01T18:15:00");

function useCountdown(targetDate: Date) {
  const [timeLeft, setTimeLeft] = useState(calculateTimeLeft());

  function calculateTimeLeft() {
    const difference = targetDate.getTime() - new Date().getTime();
    if (difference <= 0) {
      return { days: 0, hours: 0, minutes: 0, seconds: 0 };
    }
    return {
      days: Math.floor(difference / (1000 * 60 * 60 * 24)),
      hours: Math.floor((difference / (1000 * 60 * 60)) % 24),
      minutes: Math.floor((difference / 1000 / 60) % 60),
      seconds: Math.floor((difference / 1000) % 60),
    };
  }

  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft(calculateTimeLeft());
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  return timeLeft;
}

export function WeddingPage() {
  const [isPlaying, setIsPlaying] = useState(false);
  const [showRsvp, setShowRsvp] = useState(false);
  const [rsvpStatus, setRsvpStatus] = useState<"attending" | "not-attending" | null>(null);
  const audioRef = useRef<HTMLAudioElement>(null);
  const timeLeft = useCountdown(weddingDate);

  const toggleMusic = () => {
    if (audioRef.current) {
      if (isPlaying) {
        audioRef.current.pause();
      } else {
        audioRef.current.play();
      }
      setIsPlaying(!isPlaying);
    }
  };

  useEffect(() => {
    const playAudio = () => {
      if (audioRef.current) {
        audioRef.current.volume = 0.5;
        audioRef.current.play().then(() => {
          setIsPlaying(true);
        }).catch(() => {
          // Autoplay blocked, wait for user interaction
          const handleInteraction = () => {
            audioRef.current?.play().then(() => setIsPlaying(true));
            document.removeEventListener("click", handleInteraction);
          };
          document.addEventListener("click", handleInteraction);
        });
      }
    };
    playAudio();
  }, []);

  const handleRsvp = (status: "attending" | "not-attending") => {
    setRsvpStatus(status);
    setTimeout(() => setShowRsvp(false), 2000);
  };

  return (
    <div className="wedding-page">
      {/* Background Music */}
      <audio ref={audioRef} loop>
        <source src="/song.mp3" type="audio/mpeg" />
      </audio>

      {/* Music Toggle */}
      <button className="music-toggle" onClick={toggleMusic} type="button">
        <span className="material-symbols-outlined">
          {isPlaying ? "music_note" : "music_off"}
        </span>
      </button>

      {/* Floating Hearts Animation */}
      <div className="petals-container">
        {[...Array(20)].map((_, i) => (
          <div
            key={i}
            className="petal"
            style={{
              left: `${5 + (i * 4.5)}%`,
              top: `${-5 - (i % 4) * 3}%`,
              animationDelay: `-${i * 1.5}s`,
              animationDuration: `${18 + (i % 5) * 2}s`,
            }}
          />
        ))}
      </div>

      {/* Hero Section */}
      <section className="wedding-hero">
        <div className="wedding-hero__content fade-in-up">
          <span className="wedding-hero__invite">You're Invited</span>
          <div className="wedding-hero__ornament">✦</div>
          <h1 className="wedding-hero__names">
            <span>Mohammed Ziya</span>
            <span className="wedding-hero__ampersand">&</span>
            <span>Aysha Nooha</span>
          </h1>
          <p className="wedding-hero__tagline">Are getting married</p>
          <div className="wedding-hero__date">
            <span>1</span>
            <span className="wedding-hero__date-divider">•</span>
            <span>August</span>
            <span className="wedding-hero__date-divider">•</span>
            <span>2026</span>
          </div>
        </div>
        <button
          className="scroll-indicator"
          type="button"
          onClick={() => {
            document.getElementById("bismillah-section")?.scrollIntoView({ behavior: "smooth" });
          }}
        >
          <span className="material-symbols-outlined">expand_more</span>
        </button>
      </section>

      {/* Bismillah Section */}
      <section id="bismillah-section" className="wedding-section wedding-bismillah">
        <div className="wedding-card fade-in-up">
          <h2 className="arabic-text">بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ</h2>
          <p className="bismillah-translation">
            In the name of Allah, the Most Gracious, the Most Merciful
          </p>
          <div className="wedding-divider">
            <span>❧</span>
          </div>
          <p className="wedding-message">
            With the blessings of Almighty and our beloved families, we joyfully
            invite you to celebrate our Nikah and share in the happiness of this
            blessed occasion. Your presence and duas will be a cherished part of
            our special day.
          </p>
        </div>
      </section>

      {/* Countdown Section */}
      <section className="wedding-section wedding-countdown">
        <h2 className="section-title fade-in-up">Counting Down To Forever</h2>
        <div className="countdown-grid fade-in-up">
          <div className="countdown-item">
            <span className="countdown-value">{timeLeft.days}</span>
            <span className="countdown-label">Days</span>
          </div>
          <div className="countdown-item">
            <span className="countdown-value">{timeLeft.hours}</span>
            <span className="countdown-label">Hours</span>
          </div>
          <div className="countdown-item">
            <span className="countdown-value">{timeLeft.minutes}</span>
            <span className="countdown-label">Minutes</span>
          </div>
          <div className="countdown-item">
            <span className="countdown-value">{timeLeft.seconds}</span>
            <span className="countdown-label">Seconds</span>
          </div>
        </div>
      </section>

      {/* Couple Section */}
      <section className="wedding-section wedding-couple">
        <h2 className="section-title fade-in-up">The Happy Couple</h2>
        <div className="couple-layout fade-in-up">
          <div className="couple-info couple-info--groom">
            <span className="couple-label">The Groom</span>
            <h3>Mohammed Ziya</h3>
            <p>S/o Mr. Ibrahim & Mrs. Zubaida</p>
          </div>
          <div className="couple-image-center">
            <img src="/pic.png" alt="Wedding Couple" className="couple-pic" />
          </div>
          <div className="couple-info couple-info--bride">
            <span className="couple-label">The Bride</span>
            <h3>Aysha Nooha</h3>
            <p>D/o Mr. Abdul Kamal & Mrs. Nasreen</p>
          </div>
        </div>
      </section>

      {/* Events Section */}
      <section className="wedding-section wedding-events">
        <h2 className="section-title fade-in-up">Wedding Events</h2>
        <div className="events-grid fade-in-up">
          <div className="event-card">
            <div className="event-icon">
              <span className="material-symbols-outlined">diversity_1</span>
            </div>
            <h3>Nikah Ceremony</h3>
            <p className="event-date">August 1, 2026</p>
            <p className="event-time">6:15 PM</p>
            <p className="event-note">Walima Dinner followed by Nikah</p>
            <p className="event-venue">TMA Pai International Convention Centre</p>
          </div>
        </div>
      </section>

      {/* Venue Section */}
      <section className="wedding-section wedding-venue">
        <h2 className="section-title fade-in-up">Venue</h2>
        <div className="venue-card fade-in-up">
          <div className="venue-map">
            <iframe
              src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3889.5!2d74.8399!3d12.8809!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x0%3A0x0!2zMTLCsDUyJzUxLjIiTiA3NMKwNTAnMjMuNiJF!5e0!3m2!1sen!2sin!4v1234567890"
              width="100%"
              height="300"
              style={{ border: 0, borderRadius: "16px" }}
              allowFullScreen
              loading="lazy"
              title="Wedding Venue Location"
            />
          </div>
          <div className="venue-details">
            <h3>TMA Pai International Convention Centre</h3>
            <p>MG Road, Kodailbail, Mangaluru</p>
            <a
              href="https://www.google.com/maps/search/?api=1&query=12.8809,74.8399"
              target="_blank"
              rel="noopener noreferrer"
              className="venue-directions"
            >
              <span className="material-symbols-outlined">directions</span>
              Get Directions
            </a>
          </div>
        </div>
      </section>

      {/* RSVP Section */}
      <section className="wedding-section wedding-rsvp">
        <h2 className="section-title fade-in-up">Will You Join Us?</h2>
        <p className="rsvp-message fade-in-up">
          Your presence would make our special day even more memorable
        </p>
        {!showRsvp && !rsvpStatus && (
          <button
            className="rsvp-button fade-in-up"
            onClick={() => setShowRsvp(true)}
            type="button"
          >
            <span className="material-symbols-outlined">mail</span>
            RSVP Now
          </button>
        )}
        {showRsvp && !rsvpStatus && (
          <div className="rsvp-options fade-in-up">
            <button
              className="rsvp-option rsvp-option--yes"
              onClick={() => handleRsvp("attending")}
              type="button"
            >
              <span className="material-symbols-outlined">check_circle</span>
              Joyfully Accept
            </button>
            <button
              className="rsvp-option rsvp-option--no"
              onClick={() => handleRsvp("not-attending")}
              type="button"
            >
              <span className="material-symbols-outlined">cancel</span>
              Regretfully Decline
            </button>
          </div>
        )}
        {rsvpStatus && (
          <div className="rsvp-confirmation fade-in-up">
            <span className="material-symbols-outlined">
              {rsvpStatus === "attending" ? "favorite" : "sentiment_sad"}
            </span>
            <p>
              {rsvpStatus === "attending"
                ? "Thank you! We can't wait to celebrate with you!"
                : "We'll miss you! Thank you for letting us know."}
            </p>
          </div>
        )}
      </section>

      {/* Footer */}
      <footer className="wedding-footer">
        <div className="wedding-footer__content">
          <p className="wedding-footer__names">Ziya & Nooha</p>
          <p className="wedding-footer__date">01 • 08 • 2026</p>
          <p className="wedding-footer__hashtag">#ZiyaWedsNooha</p>
        </div>
      </footer>
    </div>
  );
}
