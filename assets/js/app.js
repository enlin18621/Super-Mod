const state = {
  approval: 62,
  economy: 1.8,
  budget: 32.4,
  seats: {
    unity: 42,
    progress: 31,
    green: 17,
    liberty: 10,
  },
  turn: 1,
};

const approvalEl = document.getElementById("approval");
const economyEl = document.getElementById("economy");
const budgetEl = document.getElementById("budget");
const seatsEls = {
  unity: document.getElementById("seats-unity"),
  progress: document.getElementById("seats-progress"),
  green: document.getElementById("seats-green"),
  liberty: document.getElementById("seats-liberty"),
};
const logEl = document.getElementById("log");

const priorities = [
  "Healthcare Reform",
  "Green Industrial Plan",
  "Affordable Housing",
  "Digital Education",
  "Regional Transit",
];

const voteQueue = [
  "Citizen Housing Fund",
  "Education Modernization Act",
  "Renewable Infrastructure Bond",
  "Small Business Relief",
  "Public Safety Upgrade",
];

const briefings = {
  services: [
    "Clinics expanded, waiting lists shrinking.",
    "Teachers reporting better resources.",
    "Public transport ridership climbing.",
  ],
  security: [
    "Border tensions easing.",
    "Cyber units on high alert.",
    "Police unions calm after negotiations.",
  ],
  foreign: [
    "Trade talks ongoing.",
    "Regional summit scheduled.",
    "Aid package delivered overseas.",
  ],
};

const randomBetween = (min, max) =>
  Math.round((Math.random() * (max - min) + min) * 10) / 10;

const clamp = (value, min, max) => Math.min(Math.max(value, min), max);

function log(message) {
  const entry = document.createElement("li");
  entry.textContent = `Turn ${state.turn}: ${message}`;
  logEl.prepend(entry);
}

function updateMetrics() {
  approvalEl.textContent = `${state.approval}%`;
  economyEl.textContent = `${state.economy > 0 ? "+" : ""}${state.economy.toFixed(1)}%`;
  budgetEl.textContent = `$${state.budget.toFixed(1)}B`;

  Object.entries(state.seats).forEach(([key, value]) => {
    seatsEls[key].textContent = value;
  });
}

function shuffleBriefing() {
  document.getElementById("priority").textContent =
    priorities[Math.floor(Math.random() * priorities.length)];
  document.getElementById("next-vote").textContent =
    voteQueue[Math.floor(Math.random() * voteQueue.length)];
  document.getElementById("services").textContent =
    briefings.services[Math.floor(Math.random() * briefings.services.length)];
  document.getElementById("security").textContent =
    briefings.security[Math.floor(Math.random() * briefings.security.length)];
  document.getElementById("foreign").textContent =
    briefings.foreign[Math.floor(Math.random() * briefings.foreign.length)];
}

function passLaw() {
  const boost = randomBetween(1.2, 3.5);
  state.approval = clamp(state.approval + boost, 30, 85);
  state.budget = clamp(state.budget - randomBetween(0.6, 1.8), 10, 40);
  state.economy = clamp(state.economy + randomBetween(0.2, 0.6), -2.5, 4.5);
  state.turn += 1;
  log(`Parliament passes a reform package. Approval +${boost.toFixed(1)}.`);
  shuffleBriefing();
  updateMetrics();
}

function campaignTour() {
  const boost = randomBetween(1, 2.4);
  state.approval = clamp(state.approval + boost, 30, 85);
  state.budget = clamp(state.budget - randomBetween(0.2, 0.6), 10, 40);
  state.turn += 1;
  log(`You tour swing districts and energize voters. Approval +${boost.toFixed(1)}.`);
  shuffleBriefing();
  updateMetrics();
}

function trimBudget() {
  const swing = randomBetween(0.8, 1.6);
  state.budget = clamp(state.budget + swing, 10, 40);
  state.economy = clamp(state.economy - randomBetween(0.1, 0.4), -2.5, 4.5);
  state.approval = clamp(state.approval - randomBetween(0.6, 1.4), 30, 85);
  state.turn += 1;
  log("Fiscal trims calm markets but irritate unions.");
  shuffleBriefing();
  updateMetrics();
}

function callElection() {
  const base = state.approval + state.economy * 4;
  const unitySeats = clamp(Math.round(base / 2), 30, 60);
  const progressSeats = clamp(Math.round(100 - unitySeats - randomBetween(20, 32)), 15, 40);
  const greenSeats = clamp(Math.round(randomBetween(10, 20)), 8, 22);
  const libertySeats = clamp(100 - unitySeats - progressSeats - greenSeats, 5, 20);

  state.seats = {
    unity: unitySeats,
    progress: progressSeats,
    green: greenSeats,
    liberty: libertySeats,
  };

  state.turn += 1;
  log("Election held. Coalition talks dominate the news cycle.");
  updateMetrics();
}

document.getElementById("pass-law").addEventListener("click", passLaw);
document.getElementById("campaign").addEventListener("click", campaignTour);
document.getElementById("budget-cut").addEventListener("click", trimBudget);
document.getElementById("call-election").addEventListener("click", callElection);

shuffleBriefing();
updateMetrics();
log("Your new cabinet takes office with a fragile majority.");
