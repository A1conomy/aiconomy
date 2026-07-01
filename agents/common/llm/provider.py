"""LLM provider abstraction for agent decisions."""

from abc import ABC, abstractmethod
from dataclasses import dataclass


@dataclass(frozen=True)
class LLMResponse:
    content: str


class LLMProvider(ABC):
    @abstractmethod
    def complete(self, prompt: str) -> LLMResponse:
        """Return a model completion for the given prompt."""
